package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreatePlanCommand;
import com.sep.vox.application.port.input.command.PlanQuotaInput;
import com.sep.vox.interfaces.rest.dto.request.CreatePlanRequest;

public final class CreatePlanCommandMapper {

    private CreatePlanCommandMapper() {
    }

    public static CreatePlanCommand fromRequest(CreatePlanRequest request) {
        return new CreatePlanCommand(
            request.name(),
            request.tagline(),
            request.pricePerYear(),
            request.validityDays(),
            request.maxTimePerAttemptMin(),
            request.popular(),
            request.quotas().stream()
                .map(item -> new PlanQuotaInput(item.quotaType(), item.includedQuantity(), item.tokenUnitPrice()))
                .toList()
        );
    }
}
