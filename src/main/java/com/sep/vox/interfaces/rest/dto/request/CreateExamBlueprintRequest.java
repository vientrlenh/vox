package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExamBlueprintRequest(
    @NotNull(message = "LanguageId là bắt buộc")
    UUID languageId,

    UUID schoolGradeLevelId,

    @NotBlank(message = "Mã blueprint là bắt buộc")
    String code,

    @NotBlank(message = "Tên blueprint là bắt buộc")
    String name,

    String description
) {
}
