package com.sep.vox.domain.dto;

import java.util.UUID;

public record AssessmentMemberDto(
    UUID id,
    UUID assessmentId,
    UUID userId,
    String role,
    String grantedAt,
    UUID grantedBy
) {
}
