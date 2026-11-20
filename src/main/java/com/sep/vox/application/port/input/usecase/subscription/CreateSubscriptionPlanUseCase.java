package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.CreateSubscriptionPlanQuotaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class CreateSubscriptionPlanUseCase implements IUseCase<CreateSubscriptionPlanCommand, UUID> {

    // Cột subscription_plan_quotas.included_amount_vnd là numeric(18,6) -- vượt ngưỡng này Postgres
    // báo "numeric field overflow" ở tận tầng DB thay vì một thông báo đọc được ở đây.
    private static final BigDecimal MAX_INCLUDED_AMOUNT_VND = new BigDecimal("999999999999.999999");

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final UserContextPort userContextPort;

    public CreateSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionPlanCommand input) {
        if (input.quotas() == null || input.quotas().isEmpty()) {
            throw new IllegalArgumentException("Gói phải có ít nhất một hạn mức");
        }

        CreateSubscriptionPlanCommand command = normalize(input);

        // Gom định mức đã kiểm vào map thay vì chỉ kiểm rồi bỏ: chính cái map là bộ phát hiện trùng
        // nên không cần Set riêng. LinkedHashMap để dòng hạn mức lưu xuống đúng thứ tự admin nhập.
        Map<QuotaType, BigDecimal> includedAmountByQuotaType = new LinkedHashMap<QuotaType, BigDecimal>();
        for (CreateSubscriptionPlanQuotaCommand quota : command.quotas()) {
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

        Instant now = Instant.now();
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        SubscriptionPlan plan = SubscriptionPlan.create(command.name(), command.tagline(), command.priceVnd(), command.periodType(), command.periodCount(), command.maxTimePerAttemptMin(), now, userId);
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

        // Dòng hạn mức chỉ khai ĐỊNH MỨC, không kèm đơn giá nào. Tỷ giá USD->VND được ghi theo từng
        // lượt dùng (SchoolBalanceEntry.fxRateUsed) còn phí dịch vụ là một dòng riêng trên đơn hàng,
        // nên gói không cần và không được đóng băng một tỷ giá của riêng nó.
        List<SubscriptionPlanQuota> quotas = includedAmountByQuotaType.entrySet().stream()
            .map(entry -> new SubscriptionPlanQuota(savedPlan.getId(), entry.getKey(), entry.getValue()))
            .toList();
        subscriptionPlanQuotaRepository.saveAll(quotas);

        return savedPlan.getId();
    }

    /**
     * Tên/mô tả gói hiện thẳng lên trang bán hàng nên phải gom khoảng trắng thừa: "Gói  Cơ   bản " và
     * "Gói Cơ bản" là cùng một gói, để nguyên thì admin nhìn danh sách tưởng có hai gói khác nhau.
     */
    private static CreateSubscriptionPlanCommand normalize(CreateSubscriptionPlanCommand input) {
        return new CreateSubscriptionPlanCommand(
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.tagline()),
            input.priceVnd(),
            input.periodType(),
            input.periodCount(),
            input.maxTimePerAttemptMin(),
            input.quotas()
        );
    }
}
