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
    @NotNull(message = "ID ngân hàng không được để trống")
    UUID questionBankId,

    @NotNull(message = "ID chủ đề không được để trống")
    UUID questionTopicId,

    String instructionText,

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String questionText,

    String promptText,

    String preparationText,

    @NotNull(message = "Loại câu hỏi không được để trống")
    QuestionType type,

    @NotNull(message = "Thời gian chuẩn bị không được để trống")
    @Min(value = 0, message = "Thời gian chuẩn bị không được dưới 0")
    Integer preparationTimeSeconds,

    @NotNull(message = "Thời gian trả lời tối thiểu không được để trống")
    @Min(value = 0, message = "Thời gian trả lời tối thiểu không được nhỏ hơn 0")
    Integer minResponseSeconds,

    @NotNull(message = "Thời gian trả lời tối đa không được để trống")
    @Min(value = 0, message = "Thời gian trả lời tối đa phải lớn hơn hoặc bằng 0")
    Integer maxResponseSeconds,

    QuestionSharing sharing,

    @Valid
    List<CreateQuestionAssetRequest> assets,

    @Valid
    QuestionEvaluationGuideRequest evaluationGuide
) {

    @AssertTrue(message = "Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa")
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
