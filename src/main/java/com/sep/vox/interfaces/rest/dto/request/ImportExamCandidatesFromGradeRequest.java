package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ImportExamCandidatesFromGradeRequest(
    @NotNull(message = "Khối là bắt buộc")
    UUID schoolGradeId
) {
}
