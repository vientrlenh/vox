package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import java.time.Instant;
import java.util.UUID;

public record AssessmentMemberDto(
    UUID id,
    UUID assessmentId,
    UUID userId,
    AssessmentMemberRole role,
    Instant grantedAt,
    UUID grantedBy
) {
}
