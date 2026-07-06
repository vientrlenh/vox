package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ImportExamCandidatesFromClassRequest(
    @NotNull(message = "Lớp học là bắt buộc")
    UUID schoolClassId
) {
}
