package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;

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
}
