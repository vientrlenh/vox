package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
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

    public CreateSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
    }

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionPlanCommand input) {
        if (input.quotas() == null || input.quotas().isEmpty()) {
            throw new IllegalArgumentException("Gói phải có ít nhất một hạn mức");
        }

        CreateSubscriptionPlanCommand command = normalize(input);

        // Collect the checked quotas into a map instead of only checking and discarding them: the map
        // itself is the duplicate detector, so no separate Set is needed. LinkedHashMap keeps the
        // quota rows saved in the order the admin entered them.
        Map<QuotaType, BigDecimal> includedAmountByQuotaType = new LinkedHashMap<QuotaType, BigDecimal>();
        for (CreateSubscriptionPlanQuotaCommand quota : command.quotas()) {
            QuotaType quotaType = quota.quotaType();
            // subscription_plan_quotas does NOT have a unique (subscription_plan_id, quota_type) yet, so the DB does not block it:
            // sending two GRADING rows would save both, and whichever row every later quota read picks up depends on the order
            // Postgres returns them in.
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

        SubscriptionPlan plan = SubscriptionPlan.create(command.name(), command.tagline(), command.priceVnd(), command.periodType(), command.periodCount(), command.maxTimePerAttemptMin());
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

        // A quota row only declares the QUOTA amount, with no unit price. The USD->VND exchange rate is recorded per usage
        // (SchoolBalanceEntry.fxRateUsed) and the service fee is a separate line on the order, so the plan does not need to, and
        // must not, freeze an exchange rate of its own.
        List<SubscriptionPlanQuota> quotas = includedAmountByQuotaType.entrySet().stream()
            .map(entry -> SubscriptionPlanQuota.builder()
                .subscriptionPlanId(savedPlan.getId())
                .quotaType(entry.getKey())
                .includedAmountVnd(entry.getValue())
                .build())
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
