package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamSecurePoolDto(
    UUID id,
    UUID examId,
    String status,
    String releaseMode,
    String embargoUntil,
    String releasedAt,
    UUID releasedBy
) {
}
