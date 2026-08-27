package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class UpdateSubscriptionPlanUseCase implements IUseCase<UpdateSubscriptionPlanCommand, UUID> {

    // Cột subscription_plan_quotas.included_amount_vnd là numeric(18,6) -- vượt ngưỡng này Postgres
    // báo "numeric field overflow" ở tận tầng DB thay vì một thông báo đọc được ở đây.
    private static final BigDecimal MAX_INCLUDED_AMOUNT_VND = new BigDecimal("999999999999.999999");

    // Cột subscription_plans.price_vnd là numeric(15,0), cùng độ rộng với orders.total_amount_vnd.
    private static final BigDecimal MAX_PRICE_VND = new BigDecimal("999999999999999");

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_TAGLINE_LENGTH = 2048;

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final UserContextPort userContextPort;

    public UpdateSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSubscriptionPlanCommand input) {
        var plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Gói đã publish (ACTIVE/ARCHIVED) bị khóa sửa hoàn toàn, kể cả field không liên quan
        // tiền/quota: renewal (CreatePaymentLinkForRenewalUseCase / InvoiceSettlementService) đọc
        // giá và quota LIVE từ đúng plan này, không qua snapshot nào -- sửa tại chỗ sẽ âm thầm đổi
        // giá/quota cho trường đang dùng/gia hạn mà không qua bước xác nhận. Chỉ được sửa khi còn
        // DRAFT (chưa publish nên chưa trường nào dùng); muốn đổi gói đã publish thì phải archive
        // và tạo gói mới (kèm replacedByPlanId).
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalStateException(
                "Chỉ có thể chỉnh sửa gói khi đang ở trạng thái nháp. Gói đã xuất bản thì phải lưu trữ và tạo gói mới thay thế.");
        }

        // Kiểm TOÀN BỘ đầu vào trước khi đụng vào plan: nửa chừng mà ném lỗi thì transaction rollback
        // nhưng object plan trong bộ nhớ đã bị sửa dở, và bộ hạn mức thì được xóa-ghi lại ở dưới --
        // gom phần kiểm lên đầu để không bao giờ có trạng thái sửa một nửa.
        var name = normalizedName(input.name());
        var tagline = normalizedTagline(input.tagline());
        validatePriceVnd(input.priceVnd());
        validatePeriodCount(input.periodCount());
        validateMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        var includedAmountByQuotaType = validatedQuotas(input);

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

        // KHÔNG tự tăng version ở đây: cột version do Hibernate quản lý qua @Version. Mapper dựng
        // entity MỚI mỗi lần lưu nên entity luôn detached, save() đi đường merge và merge SO version
        // của entity với version dưới DB -- tăng sẵn thành N+1 trong khi DB còn N thì mọi lần sửa đều
        // bị coi là ghi đè lên bản cũ và ném OptimisticLockException.
        plan.setUpdatedAt(Instant.now());
        plan.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var savedPlan = subscriptionPlanRepository.save(plan);

        if (includedAmountByQuotaType != null) {
            // Thay TOÀN BỘ bộ hạn mức chứ không sửa từng dòng: dòng hạn mức không có khóa nghiệp vụ
            // nào ngoài (plan, quotaType), và gói còn DRAFT nên chưa có gì tham chiếu tới id của
            // chúng -- xóa hết rồi ghi lại là cách duy nhất diễn đạt được "bỏ hẳn loại hạn mức này".
            subscriptionPlanQuotaRepository.deleteBySubscriptionPlanId(savedPlan.getId());
            var quotas = includedAmountByQuotaType.entrySet().stream()
                .map(entry -> new SubscriptionPlanQuota(savedPlan.getId(), entry.getKey(), entry.getValue()))
                .toList();
            subscriptionPlanQuotaRepository.saveAll(quotas);
        }

        // Hạn mức không nằm trong SubscriptionPlanDto -- client GraphQL nào cần thì chọn trường
        // quotas và resolver nạp qua DataLoader quotasBySubscriptionPlanId.
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
        var normalized = StringNormalization.trimAndCollapseSpaces(name);
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
        var normalized = StringNormalization.trimAndCollapseSpaces(tagline);
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

        var includedAmountByQuotaType = new LinkedHashMap<QuotaType, BigDecimal>();
        for (var quota : input.quotas()) {
            var quotaType = quotaTypeOf(quota.quotaType());
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

    // Command nhận chuỗi thô để giữ nguyên hình dạng payload; schema GraphQL khai QuotaType! nên trên
    // đường GraphQL giá trị lạ đã bị chặn từ khâu parse, nhưng use case vẫn tự kiểm để còn dùng được
    // từ lối vào khác (REST, job) mà không phụ thuộc vào tầng interface.
    private static QuotaType quotaTypeOf(String quotaType) {
        if (quotaType == null) {
            throw new IllegalArgumentException("Loại hạn mức không được để trống");
        }
        try {
            return QuotaType.valueOf(quotaType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại hạn mức không hợp lệ: " + quotaType);
        }
    }
}
