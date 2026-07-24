package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.PlanQuotaInput;
import com.sep.vox.application.port.input.command.UpdatePlanCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSubscriptionPlanInput;

public final class UpdateSubscriptionPlanCommandMapper {

    private UpdateSubscriptionPlanCommandMapper() {
    }

    public static UpdatePlanCommand fromInput(UUID id, UpdateSubscriptionPlanInput input) {
        return new UpdatePlanCommand(
            id,
            input.name(),
            input.tagline(),
            input.pricePerYear(),
            input.validityDays(),
            input.maxTimePerAttemptMin(),
            input.maxStudentCount(),
            input.popular(),
            input.quotas() == null
                ? null
                : input.quotas().stream()
                    .map(item -> new PlanQuotaInput(item.quotaType(), item.includedQuantity(), item.tokenUnitPrice()))
                    .toList()
        );
    }
}
