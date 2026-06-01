package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
    @NotNull(message = "ID chủ đề không được để trống")
    UUID topicId,

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String questionText,

    @Size(max = 512, message = "URL audio không được vượt quá 512 ký tự")
    String audioUrl,

    @NotNull(message = "ID cấp độ chuẩn không được để trống")
    UUID standardLevelId,

    @NotBlank(message = "Loại câu hỏi không được để trống")
    String questionType,

    @NotNull(message = "Thời lượng không được để trống")
    @Positive(message = "Thời lượng phải lớn hơn 0")
    Integer durationSeconds
) {
}
