package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanQuotaCommand;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class UpdateSubscriptionPlanUseCase implements IUseCase<UpdateSubscriptionPlanCommand, UUID> {

    // Column subscription_plan_quotas.included_amount_vnd is numeric(18,6) -- above this limit Postgres
    // reports "numeric field overflow" at the DB layer instead of a readable message here.
    private static final BigDecimal MAX_INCLUDED_AMOUNT_VND = new BigDecimal("999999999999.999999");

    // Column subscription_plans.price_vnd is numeric(15,0), the same width as orders.total_amount_vnd.
    private static final BigDecimal MAX_PRICE_VND = new BigDecimal("999999999999999");

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_TAGLINE_LENGTH = 2048;

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSubscriptionPlanCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói"));

        // A published plan (ACTIVE/ARCHIVED) is locked against editing entirely, even fields unrelated to money/quota: renewal
        // (CreatePaymentLinkForRenewalUseCase / InvoiceSettlementService) reads the price and quota LIVE from this very plan, with
        // no snapshot -- editing in place would silently change the price/quota for a school that is using/renewing without any
        // confirmation step. It can only be edited while DRAFT (not published, so no school uses it yet); to change a published
        // plan, archive it and create a new plan (with replacedByPlanId).
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalArgumentException(
                "Chỉ có thể chỉnh sửa gói khi đang ở trạng thái nháp. Gói đã xuất bản thì phải lưu trữ và tạo gói mới thay thế.");
        }

        // Validate ALL input before touching the plan: if an error is thrown halfway, the transaction rolls back but the plan
        // object in memory has already been half-edited, and the quota set is deleted and rewritten below -- gathering the
        // checks at the top ensures there is never a half-edited state.
        String name = normalizedName(input.name());
        String tagline = normalizedTagline(input.tagline());
        validatePriceVnd(input.priceVnd());
        validatePeriodCount(input.periodCount());
        validateMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        Map<QuotaType, BigDecimal> includedAmountByQuotaType = validatedQuotas(input);

        if (name != null) {
            plan.setName(name);
        }
        if (tagline != null) {
            plan.setTagline(tagline);
        }
        if (input.priceVnd() != null) {
            plan.setPriceVnd(input.priceVnd());
        }
        if (input.periodCount() != null) {
            plan.setPeriodCount(input.periodCount());
        }
        if (input.maxTimePerAttemptMin() != null) {
            plan.setMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        }

        // Do NOT increment the version here: the version column is managed by Hibernate through @Version. The mapper builds a NEW
        // entity on every save so the entity is always detached, save() goes through merge, and merge COMPARES the entity's version
        // with the version in the DB -- incrementing it to N+1 while the DB still holds N makes every edit look like an overwrite of
        // an older copy and throws OptimisticLockException.
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

        if (includedAmountByQuotaType != null) {
            // Replace the WHOLE quota set instead of editing row by row: a quota row has no business key other than (plan, quotaType),
            // and the plan is still DRAFT so nothing references their ids yet -- deleting everything and writing again is the only
            // way to express "drop this quota type entirely".
            subscriptionPlanQuotaRepository.deleteBySubscriptionPlanId(savedPlan.getId());
            List<SubscriptionPlanQuota> quotas = includedAmountByQuotaType.entrySet().stream()
                .map(entry -> SubscriptionPlanQuota.builder()
                    .subscriptionPlanId(savedPlan.getId())
                    .quotaType(entry.getKey())
                    .includedAmountVnd(entry.getValue())
                    .build())
                .toList();
            subscriptionPlanQuotaRepository.saveAll(quotas);
        }

        // Quota is not part of SubscriptionPlanDto -- a GraphQL client that needs it selects the quotas field, and the resolver
        // loads it through the subscriptionPlanQuotasBySubscriptionPlanId DataLoader.
        return savedPlan.getId();
    }

    /**
     * Tên gói hiện thẳng lên trang bán hàng nên phải gom khoảng trắng thừa: "Gói  Cơ   bản " và
     * "Gói Cơ bản" là cùng một gói, để nguyên thì admin nhìn danh sách tưởng có hai gói khác nhau.
     *
     * <p>Chuỗi rỗng/toàn khoảng trắng bị từ chối chứ không lặng lẽ bỏ qua: cột name là NOT NULL và
     * người gửi rõ ràng đang CÓ ý sửa tên -- coi như "không sửa" thì họ tưởng đã xóa được tên.
     */
    private static String normalizedName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = StringNormalization.trimAndCollapseSpaces(name);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Tên gói không được để trống");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên gói không được vượt quá " + MAX_NAME_LENGTH + " ký tự");
        }
        return normalized;
    }

    private static String normalizedTagline(String tagline) {
        if (tagline == null) {
            return null;
        }
        String normalized = StringNormalization.trimAndCollapseSpaces(tagline);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Mô tả gói không được để trống");
        }
        if (normalized.length() > MAX_TAGLINE_LENGTH) {
            throw new IllegalArgumentException("Mô tả gói không được vượt quá " + MAX_TAGLINE_LENGTH + " ký tự");
        }
        return normalized;
    }

    /**
     * price_vnd là numeric(15,0) -- SCALE 0 chứ không phải làm tròn cho đẹp. Gửi 100000.5 xuống thì
     * Postgres LÀM TRÒN IM LẶNG thành 100001: gói niêm yết một giá mà thu một giá khác, không có lỗi
     * nào báo ra. Chặn ở đây để admin biết mình vừa gõ thừa phần thập phân.
     */
    private static void validatePriceVnd(BigDecimal priceVnd) {
        if (priceVnd == null) {
            return;
        }
        if (priceVnd.signum() < 0) {
            throw new IllegalArgumentException("Giá gói không được âm");
        }
        if (priceVnd.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Giá gói phải là số nguyên VND, không có phần thập phân");
        }
        if (priceVnd.compareTo(MAX_PRICE_VND) > 0) {
            throw new IllegalArgumentException("Giá gói vượt quá giới hạn cho phép (tối đa 999.999.999.999.999 VND)");
        }
    }

    // Khớp CHECK chk_subscription_plans_period_count_positive dưới DB -- chặn ở đây để ra thông báo
    // đọc được thay vì một ConstraintViolationException từ tầng JDBC.
    private static void validatePeriodCount(Integer periodCount) {
        if (periodCount != null && periodCount < 1) {
            throw new IllegalArgumentException("Số chu kỳ của gói phải lớn hơn 0");
        }
    }

    // Khớp CHECK chk_subscription_plans_max_time_per_attempt_min_positive dưới DB.
    private static void validateMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        if (maxTimePerAttemptMin != null && maxTimePerAttemptMin < 1) {
            throw new IllegalArgumentException("Số phút của một bài kiểm tra phải lớn hơn 0");
        }
    }

    /**
     * Trả về null khi lệnh không đụng tới hạn mức, để chỗ gọi phân biệt được "giữ nguyên" với "thay
     * bộ mới". LinkedHashMap để dòng hạn mức lưu xuống đúng thứ tự admin nhập, và chính cái map là bộ
     * phát hiện trùng nên không cần Set riêng.
     */
    private static LinkedHashMap<QuotaType, BigDecimal> validatedQuotas(UpdateSubscriptionPlanCommand input) {
        if (input.quotas() == null) {
            return null;
        }
        // Danh sách rỗng KHÔNG phải là "xóa hết": gói không còn hạn mức nào thì trường mua về không
        // dùng được gì, mà CreateSubscriptionPlanUseCase cũng đã bắt buộc có ít nhất một hạn mức.
        // Muốn không đụng tới hạn mức thì bỏ hẳn field quotas ra khỏi input.
        if (input.quotas().isEmpty()) {
            throw new IllegalArgumentException("Gói phải có ít nhất một hạn mức");
        }

        LinkedHashMap<QuotaType, BigDecimal> includedAmountByQuotaType = new LinkedHashMap<QuotaType, BigDecimal>();
        for (UpdateSubscriptionPlanQuotaCommand quota : input.quotas()) {
            QuotaType quotaType = quota.quotaType();
            // subscription_plan_quotas CHƯA có unique (subscription_plan_id, quota_type), nên DB
            // không chặn giúp: gửi hai dòng GRADING sẽ lưu cả hai và mọi chỗ đọc hạn mức về sau lấy
            // trúng dòng nào là tùy thứ tự trả về của Postgres.
            if (includedAmountByQuotaType.containsKey(quotaType)) {
                throw new IllegalArgumentException(
                    "Loại hạn mức \"" + quotaType + "\" bị khai báo nhiều lần trong cùng một gói");
            }
            if (quota.includedAmountVnd() == null) {
                throw new IllegalArgumentException("Định mức của hạn mức \"" + quotaType + "\" không được để trống");
            }
            if (quota.includedAmountVnd().signum() < 0) {
                throw new IllegalArgumentException("Định mức của hạn mức \"" + quotaType + "\" không được âm");
            }
            if (quota.includedAmountVnd().compareTo(MAX_INCLUDED_AMOUNT_VND) > 0) {
                throw new IllegalArgumentException(
                    "Định mức của hạn mức \"" + quotaType + "\" vượt quá giới hạn cho phép (tối đa 999.999.999.999 VND)");
            }
            includedAmountByQuotaType.put(quotaType, quota.includedAmountVnd());
        }
        return includedAmountByQuotaType;
    }
}
