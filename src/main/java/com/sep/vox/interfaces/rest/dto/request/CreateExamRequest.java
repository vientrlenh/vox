package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExamRequest(
    @NotBlank(message = "Mã bài kiểm tra là bắt buộc")
    String code,

    @NotBlank(message = "Tên bài kiểm tra là bắt buộc")
    String name,

    String description,

    @NotNull(message = "Ngôn ngữ là bắt buộc")
    UUID languageId,

    UUID blueprintId,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId
) {
}
