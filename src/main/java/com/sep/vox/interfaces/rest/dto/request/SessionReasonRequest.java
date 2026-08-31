package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionReasonRequest(
    @NotBlank(message = "Lý do là bắt buộc")
    @Size(max = 512, message = "Lý do không được vượt quá 512 ký tự")
    String reason
) {
}
