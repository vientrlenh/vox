package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExamBlueprintSectionItemRequest(
    @NotNull(message = "Thứ tự section là bắt buộc")
    Integer order,

    @NotBlank(message = "Tiêu đề section là bắt buộc")
    String title,

    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight
) {
}
