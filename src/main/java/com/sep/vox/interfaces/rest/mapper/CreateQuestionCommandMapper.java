package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankQuestionRequest;

public final class CreateQuestionCommandMapper {

    private CreateQuestionCommandMapper() {
    }

    public static CreateSystemQuestionBankQuestionCommand fromQuestionBankRequest(CreateSystemQuestionBankQuestionRequest request) {
        return new CreateSystemQuestionBankQuestionCommand(
            request.questionTopicId(),
            request.code(),
            request.instructionText(),
            request.questionText(),
            request.promptText(),
            request.preparationText(),
            request.type(),
            request.scope(),
            request.visibility(),
            request.preparationTimeSeconds(),
            request.minResponseSeconds(),
            request.maxResponseSeconds()
        );
    }

    public static CreateSchoolQuestionBankQuestionCommand fromSchoolRequest(CreateSystemQuestionBankQuestionRequest request) {
        return new CreateSchoolQuestionBankQuestionCommand(
            request.questionTopicId(),
            request.code(),
            request.instructionText(),
            request.questionText(),
            request.promptText(),
            request.preparationText(),
            request.type(),
            request.scope(),
            request.visibility(),
            request.preparationTimeSeconds(),
            request.minResponseSeconds(),
            request.maxResponseSeconds()
        );
    }
}
