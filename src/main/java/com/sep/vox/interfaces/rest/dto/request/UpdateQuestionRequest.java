package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateQuestionRequest(
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    String type,
    @Min(value = 0, message = "Thời gian chuẩn bị không được nhỏ hơn 0")
    Integer preparationTimeSeconds,
    @Min(value = 0, message = "Thời gian trả lời tối thiểu không được nhỏ hơn 0")
    Integer minResponseSeconds,
    @Min(value = 0, message = "Thời gian trả lời tối đa không được nhỏ hơn 0")
    Integer maxResponseSeconds,
    @Size(max = 20, message = "Chế độ chia sẻ không hợp lệ")
    String sharing
) {
}
