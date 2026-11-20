package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateAssessmentCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentRequest;

public final class CreateAssessmentCommandMapper {

    private CreateAssessmentCommandMapper() {
    }

    public static CreateAssessmentCommand fromRequest(CreateAssessmentRequest request) {
        return new CreateAssessmentCommand(
            request.code(),
            request.name(),
            request.description(),
            request.languageId(),
            request.blueprintId(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.assessmentTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiresOtp(), 
            request.requiredStreamTypes(),
            request.streamTypePermission(),
            request.deliveryMode(),
            request.aiConfidenceThresholdPercent()
        );
    }
}
