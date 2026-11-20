package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ExamPaperAssignmentRequestItem(
    @NotNull(message = "Thí sinh là bắt buộc")
    UUID candidateId,
    @NotNull(message = "Mã đề là bắt buộc")
    UUID paperId
) {
}
