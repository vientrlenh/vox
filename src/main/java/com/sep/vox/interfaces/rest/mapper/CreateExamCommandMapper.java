package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateExamCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamRequest;

public final class CreateExamCommandMapper {

    private CreateExamCommandMapper() {
    }

    public static CreateExamCommand fromRequest(CreateExamRequest request) {
        return new CreateExamCommand(
            request.code(),
            request.name(),
            request.description(),
            request.languageId(),
            request.blueprintId(),
            request.openAt(),
            request.closeAt(),
            request.assessmentPolicyId(),
            request.maxAttempt(),
            request.examTimeDurationSecond(),
            request.resultDecisionMethod(),
            request.requiresOtp()
        );
    }
}
