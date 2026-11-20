package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamRequest;

public final class UpdateExamCommandMapper {

    private UpdateExamCommandMapper() {
    }

    public static UpdateExamCommand fromRequest(UUID examId, UpdateExamRequest request) {
        return new UpdateExamCommand(
            examId,
            request.name(),
            request.description(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.examTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiresOtp(),
            request.requiredStreamTypes(),
            request.streamTypePermission()
,
            request.aiConfidenceThresholdPercent()
        );
    }
}
