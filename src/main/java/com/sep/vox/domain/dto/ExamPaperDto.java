package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamPaperDto(
    UUID id,
    UUID examId,
    UUID blueprintVersionId,
    String code,
    int variant,
    String status,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
