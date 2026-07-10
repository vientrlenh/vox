package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateClassTestSectionRequest(
    @NotBlank(message = "Tiêu đề section là bắt buộc")
    String title,

    String instruction,

    @NotEmpty(message = "Section phải có ít nhất một câu hỏi")
    List<UUID> questionIds
) {
}
