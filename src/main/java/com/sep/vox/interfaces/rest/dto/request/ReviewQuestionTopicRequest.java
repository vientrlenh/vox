package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewQuestionTopicRequest(
    @NotBlank(message = "Trạng thái mục tiêu không được để trống")
    String targetStatus
) {
}
