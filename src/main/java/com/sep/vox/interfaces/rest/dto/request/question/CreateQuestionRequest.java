package com.sep.vox.interfaces.rest.dto.request.question;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.question.CreateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.question.CreateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.command.question.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionRequest(
    @NotNull(message = "Bank ID must not be blank")
    UUID questionBankId,

    @NotNull(message = "Topic ID must not be blank")
    UUID questionTopicId,

    String instructionText,

    @NotBlank(message = "Question content must not be blank")
    String questionText,

    String promptText,

    String preparationText,

    @NotNull(message = "Question type must not be blank")
    QuestionType type,

    @NotNull(message = "Preparation time must not be blank")
    @Min(value = 0, message = "Preparation time must not be below 0")
    Integer preparationTimeSeconds,

    @NotNull(message = "Minimum answer time must not be blank")
    @Min(value = 0, message = "Minimum answer time must not be less than 0")
    Integer minResponseSeconds,

    @NotNull(message = "Maximum answer time must not be blank")
    @Min(value = 0, message = "Maximum answer time must be greater than or equal to 0")
    Integer maxResponseSeconds,

    QuestionSharing sharing,

    @Valid
    List<CreateQuestionAssetRequest> assets,

    @Valid
    QuestionEvaluationGuideRequest evaluationGuide
) {

    @AssertTrue(message = "Minimum answer time must not be greater than maximum answer time")
    public boolean isResponseDurationRangeValid() {
        return minResponseSeconds == null
            || maxResponseSeconds == null
            || minResponseSeconds <= maxResponseSeconds;
    }

    public static CreateSystemQuestionBankQuestionCommand toCommand(CreateQuestionRequest request) {
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
            .map(CreateQuestionRequest::toAssetCommand)
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
