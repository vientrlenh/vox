package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record QuestionBankGradeDto(
    UUID id,
    UUID questionBankId,
    UUID schoolGradeId,
    Instant attachedAt,
    UUID attachedBy
) {
}
