package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamMemberDto(
    UUID id,
    UUID examId,
    UUID userId,
    String role,
    String grantedAt,
    UUID grantedBy
) {
}
