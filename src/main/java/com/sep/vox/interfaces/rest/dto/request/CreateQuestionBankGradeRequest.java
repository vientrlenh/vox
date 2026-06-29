package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateQuestionBankGradeRequest(
    @NotNull(message = "ID khối lớp không được để trống")
    UUID schoolGradeId
) {
}
