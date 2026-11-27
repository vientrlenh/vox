package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanQuotaCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanQuotaAmountValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class CreateSubscriptionPlanUseCase implements IUseCase<CreateSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final SubscriptionPlanQuotaAmountValidator quotaAmountValidator;

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionPlanCommand input) {
        if (input.quotas() == null || input.quotas().isEmpty()) {
            throw new IllegalArgumentException("Gói phải có ít nhất một hạn mức");
        }

        CreateSubscriptionPlanCommand command = normalize(input);
        Map<QuotaType, BigDecimal> includedAmountByQuotaType = validatedIncludedAmounts(command.quotas());

        SubscriptionPlan plan = SubscriptionPlan.create(command.name(), command.tagline(), command.priceVnd(), command.periodType(), command.periodCount(), command.maxTimePerAttemptMin());
        SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);
        savePlanQuotas(savedPlan.getId(), includedAmountByQuotaType);

        return savedPlan.getId();
    }

    private Map<QuotaType, BigDecimal> validatedIncludedAmounts(List<CreateSubscriptionPlanQuotaCommand> quotas) {
        // Collect the checked quotas into a map instead of only checking and discarding them: the map
        // itself is the duplicate detector, so no separate Set is needed. LinkedHashMap keeps the
        // quota rows saved in the order the admin entered them.
        Map<QuotaType, BigDecimal> includedAmountByQuotaType = new LinkedHashMap<QuotaType, BigDecimal>();
        for (CreateSubscriptionPlanQuotaCommand quota : quotas) {
            QuotaType quotaType = quota.quotaType();
            // subscription_plan_quotas does NOT have a unique (subscription_plan_id, quota_type) yet, so the DB does not block it:
            // sending two GRADING rows would save both, and whichever row every later quota read picks up depends on the order
            // Postgres returns them in.
            if (includedAmountByQuotaType.containsKey(quotaType)) {
                throw new IllegalArgumentException(
                    "Loại hạn mức \"" + quotaType + "\" bị khai báo nhiều lần trong cùng một gói");
            }
            quotaAmountValidator.requireValidIncludedAmount(quotaType, quota.includedAmountVnd());
            includedAmountByQuotaType.put(quotaType, quota.includedAmountVnd());
        }
        return includedAmountByQuotaType;
    }

    // A quota row only declares the QUOTA amount, with no unit price. The USD->VND exchange rate is recorded per usage
    // (SchoolBalanceEntry.fxRateUsed) and the service fee is a separate line on the order, so the plan does not need to, and
    // must not, freeze an exchange rate of its own.
    private void savePlanQuotas(UUID planId, Map<QuotaType, BigDecimal> includedAmountByQuotaType) {
        List<SubscriptionPlanQuota> quotas = includedAmountByQuotaType.entrySet().stream()
            .map(entry -> SubscriptionPlanQuota.forPlan(planId, entry.getKey(), entry.getValue()))
            .toList();
        subscriptionPlanQuotaRepository.saveAll(quotas);
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
