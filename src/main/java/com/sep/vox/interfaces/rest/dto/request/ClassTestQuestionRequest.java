package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ClassTestQuestionRequest(
    @NotNull(message = "questionId là bắt buộc")
    UUID questionId,

    @NotNull(message = "weight là bắt buộc")
    BigDecimal weight
) {
}
