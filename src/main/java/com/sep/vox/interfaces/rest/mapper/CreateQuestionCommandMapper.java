package com.sep.vox.interfaces.rest.mapper;

import java.util.List;

import com.sep.vox.application.port.input.command.CreateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.CreateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionAssetRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.QuestionEvaluationGuideRequest;

public final class CreateQuestionCommandMapper {

    private CreateQuestionCommandMapper() {
    }

    public static CreateSystemQuestionBankQuestionCommand fromRequest(CreateQuestionRequest request) {
        return new CreateSystemQuestionBankQuestionCommand(
            request.questionBankId(),
            request.questionTopicId(),
            null,
            request.instructionText(),
            request.questionText(),
            request.promptText(),
            request.preparationText(),
            request.type(),
            request.preparationTimeSeconds(),
            request.minResponseSeconds(),
            request.maxResponseSeconds(),
            request.sharing(),
            toAssetCommands(request.assets()),
            toEvaluationGuideCommand(request.evaluationGuide())
        );
    }

    private static List<CreateQuestionAssetCommand> toAssetCommands(List<CreateQuestionAssetRequest> requests) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
            .map(CreateQuestionCommandMapper::toAssetCommand)
            .toList();
    }

    private static CreateQuestionAssetCommand toAssetCommand(CreateQuestionAssetRequest request) {
        return new CreateQuestionAssetCommand(
            request.title(),
            request.durationSeconds(),
            request.altText(),
            request.type(),
            request.url(),
            request.transcript(),
            request.description(),
            request.order()
        );
    }

    private static CreateQuestionEvaluationGuideCommand toEvaluationGuideCommand(QuestionEvaluationGuideRequest request) {
        if (request == null) {
            return null;
        }

        return new CreateQuestionEvaluationGuideCommand(
            request.expectedContent(),
            request.keyPoints(),
            request.acceptableResponses(),
            request.offTopicExamples(),
            request.scoringHints(),
            request.commonMistakes()
        );
    }
}
