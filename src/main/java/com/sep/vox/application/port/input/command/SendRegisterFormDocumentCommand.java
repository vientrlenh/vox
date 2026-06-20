package com.sep.vox.application.port.input.command;

import jakarta.validation.constraints.NotBlank;

public record SendRegisterFormDocumentCommand(
    @NotBlank(message = "Đường dẫn của tài liệu không được để trống")
    String url
) {
    
}
