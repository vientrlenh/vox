package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;

public record AssessmentMemberDto(
    UUID id,
    UUID assessmentId,
    UUID userId,
    AssessmentMemberRole role,
    Instant grantedAt,
    UUID grantedBy
) {

    public static AssessmentMemberDto toDto(AssessmentMember assessmentMember) {
        return new AssessmentMemberDto(
            assessmentMember.getId(),
            assessmentMember.getAssessmentId(),
            assessmentMember.getUserId(),
            assessmentMember.getRole(),
            assessmentMember.getGrantedAt(),
            assessmentMember.getGrantedBy()
        );
    }

    public static List<AssessmentMemberDto> toDtoList(List<AssessmentMember> domains) {
        return domains.stream()
            .map(AssessmentMemberDto::toDto)
            .toList();
    }
}
