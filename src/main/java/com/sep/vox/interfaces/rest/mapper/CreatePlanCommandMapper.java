package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.CreateSubscriptionPlanQuotaCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSubscriptionPlanRequest;

public final class CreatePlanCommandMapper {

    private CreatePlanCommandMapper() {
    }

    public static CreateSubscriptionPlanCommand fromRequest(CreateSubscriptionPlanRequest request) {
        return new CreateSubscriptionPlanCommand(
            request.name(),
            request.tagline(),
            request.priceVnd(),
            request.periodType(),
            request.periodCount(),
            request.maxTimePerAttemptMin(),
            request.quotas().stream()
                .map(item -> new CreateSubscriptionPlanQuotaCommand(item.quotaType(), item.includedAmountVnd()))
                .toList()
        );
    }
}
