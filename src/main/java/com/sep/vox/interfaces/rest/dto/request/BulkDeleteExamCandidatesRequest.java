package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record BulkDeleteExamCandidatesRequest(
    @NotEmpty(message = "Danh sách thí sinh không được để trống")
    List<UUID> candidateIds
) {
}
