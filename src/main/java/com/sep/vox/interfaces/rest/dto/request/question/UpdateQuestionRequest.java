package com.sep.vox.interfaces.rest.dto.request.question;

import com.sep.vox.application.port.input.command.question.UpdateQuestionCommand;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;
import java.util.UUID;

import jakarta.validation.constraints.Min;

public record UpdateQuestionRequest(
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    QuestionType type,
    @Min(value = 0, message = "Thời gian chuẩn bị không được nhỏ hơn 0")
    Integer preparationTimeSeconds,
    @Min(value = 0, message = "Thời gian trả lời tối thiểu không được nhỏ hơn 0")
    Integer minResponseSeconds,
    @Min(value = 0, message = "Thời gian trả lời tối đa không được nhỏ hơn 0")
    Integer maxResponseSeconds,
    QuestionSharing sharing
) {

    public static UpdateQuestionCommand toCommand(UUID id, UpdateQuestionRequest request) {
        return new UpdateQuestionCommand(
            id,
            request.instructionText(),
            request.questionText(),
            request.promptText(),
            request.preparationText(),
            request.type(),
            request.preparationTimeSeconds(),
            request.minResponseSeconds(),
            request.maxResponseSeconds(),
            request.sharing()
        );
    }
}
