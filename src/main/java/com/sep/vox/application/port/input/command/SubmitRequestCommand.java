package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.subscription.RequestType;

public record SubmitRequestCommand(
    UUID schoolId,
    RequestType requestType,
    UUID currentPlanId,
    UUID requestedPlanId,
    String idempotencyKey
) {
}
