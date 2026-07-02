package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ExamBlueprintSectionRequest(
    UUID id,

    @NotNull(message = "Thứ tự section là bắt buộc")
    Integer order,

    @NotBlank(message = "Tiêu đề section là bắt buộc")
    String title,

    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight,

    @Valid
    @NotEmpty(message = "Section phải có ít nhất một slot")
    List<ExamBlueprintSlotRequest> slots
) {
}
