package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateQuestionTopicRequest(
    @NotNull(message = "ID ngân hàng câu hỏi không được để trống")
    UUID bankId,

    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 255, message = "Tên chủ đề không được vượt quá 255 ký tự")
    String topicName,

    @Size(max = 2048, message = "Mô tả không được vượt quá 2048 ký tự")
    String description
) {
}
