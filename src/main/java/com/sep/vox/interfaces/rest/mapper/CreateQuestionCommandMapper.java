package com.sep.vox.interfaces.rest.mapper;

import java.util.List;

import com.sep.vox.application.port.input.command.CreateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.CreateQuestionCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionAssetRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionRequest;

public final class CreateQuestionCommandMapper {

    public static CreateQuestionCommand fromRequest(CreateQuestionRequest request) {
        return new CreateQuestionCommand(
            request.questionTopicId(),
            request.code(),
            request.instructionText(),
            request.questionText(),
            request.promptText(),
            request.preparationText(),
            request.standardLevelVersionId(),
            request.schoolLevelVersionId(),
            request.expectedContent(),
            request.keyPoints(),
            request.acceptableResponses(),
            request.offTopicExamples(),
            request.scoringHints(),
            request.commonMistakes(),
            request.type(),
            request.preparationTimeSeconds(),
            request.minResponseSeconds(),
            request.maxResponseSeconds(),
            request.status(),
            toAssetCommands(request.assets())
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
}
