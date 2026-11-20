package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionBankGradeDto(
    UUID id,
    UUID questionBankId,
    UUID schoolGradeId,
    String attachedAt,
    UUID attachedBy
) {
}
