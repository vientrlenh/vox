package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewQuestionRequest(
    @NotBlank(message = "Trạng thái đích không được để trống") String targetStatus,
    String note,
    String reason
) {
}
