package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionContentRequest(
    String instructionText,
    @NotBlank(message = "Nội dung câu hỏi không được để trống") String questionText,
    String promptText,
    String preparationText,
    @NotBlank(message = "Loại câu hỏi không được để trống") String type,
    @Min(value = 0, message = "Thời gian chuẩn bị phải >= 0") int preparationTimeSeconds,
    @Min(value = 0, message = "Thời gian trả lời tối thiểu phải >= 0") int minResponseSeconds,
    @Min(value = 0, message = "Thời gian trả lời tối đa phải >= 0") int maxResponseSeconds
) {
}
