package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentRequest;

public final class UpdateAssessmentCommandMapper {

    private UpdateAssessmentCommandMapper() {
    }

    public static UpdateAssessmentCommand fromRequest(UUID assessmentId, UpdateAssessmentRequest request) {
        return new UpdateAssessmentCommand(
            assessmentId,
            request.name(),
            request.description(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.assessmentTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiresOtp(),
            request.requiredStreamTypes(),
            request.streamTypePermission()
,
            request.aiConfidenceThresholdPercent()
        );
    }
}
