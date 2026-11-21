package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentMemberJpaEntity;

public final class AssessmentMemberMapper {

    private AssessmentMemberMapper() {}

    public static AssessmentMember toDomain(AssessmentMemberJpaEntity jpa) {
        return AssessmentMember.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .userId(jpa.getUserId())
                .role(roleFromString(jpa.getRole()))
                .grantedAt(jpa.getGrantedAt())
                .grantedBy(jpa.getGrantedBy())
                .build();
    }

    public static AssessmentMemberJpaEntity toJpa(AssessmentMember domain) {
        return AssessmentMemberJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .userId(domain.getUserId())
                .role(domain.getRole().name())
                .grantedAt(domain.getGrantedAt())
                .grantedBy(domain.getGrantedBy())
                .build();
    }

    private static AssessmentMemberRole roleFromString(String role) {
        return role == null ? null : AssessmentMemberRole.valueOf(role);
    }
}
