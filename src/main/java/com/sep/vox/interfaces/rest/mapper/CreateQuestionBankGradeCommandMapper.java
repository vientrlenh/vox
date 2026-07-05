package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionBankGradeCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionBankGradeRequest;

public final class CreateQuestionBankGradeCommandMapper {

    private CreateQuestionBankGradeCommandMapper() {
    }

    public static CreateQuestionBankGradeCommand fromRequest(UUID questionBankId, CreateQuestionBankGradeRequest request) {
        return new CreateQuestionBankGradeCommand(questionBankId, request.schoolGradeId());
    }
}
