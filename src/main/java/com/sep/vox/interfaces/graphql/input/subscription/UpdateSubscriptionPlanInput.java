package com.sep.vox.interfaces.graphql.input.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanQuotaCommand;

/**
 * Matches the input UpdateSubscriptionPlanInput in subscription.graphqls: every field is nullable with the
 * meaning "do not touch". There is no periodType (the column period_type is updatable = false) and no
 * serviceFeeRatio (it became a system-wide config) -- see {@link
 * com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanCommand}.
 */
public record UpdateSubscriptionPlanInput(
    ArgumentValue<String> name,
    ArgumentValue<String> tagline,
    ArgumentValue<BigDecimal> priceVnd,
    ArgumentValue<Integer> periodCount,
    ArgumentValue<Integer> maxTimePerAttemptMin,
    ArgumentValue<List<SubscriptionPlanQuotaInput>> quotas
) {

    public static UpdateSubscriptionPlanCommand toCommand(UUID id, UpdateSubscriptionPlanInput input) {
        return new UpdateSubscriptionPlanCommand(
            id,
            input.name().value(),
            input.tagline().value(),
            input.priceVnd().value(),
            input.periodCount().value(),
            input.maxTimePerAttemptMin().value(),
            // null (do not change the quotas) is very different from an empty list (clear all the quotas), so null must be
            // passed unchanged down to the use case instead of being turned into List.of().
            input.quotas().value() == null
                ? null
                : input.quotas().value().stream()
                    .map(item -> new UpdateSubscriptionPlanQuotaCommand(
                        item.quotaType(),
                        item.includedAmountVnd()
                    ))
                    .toList()
        );
    }
}
