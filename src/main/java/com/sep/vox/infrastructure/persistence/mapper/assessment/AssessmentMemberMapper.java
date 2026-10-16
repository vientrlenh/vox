package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentMemberJpaEntity;

public final class AssessmentMemberMapper {

    private AssessmentMemberMapper() {}

    public static AssessmentMember toDomain(AssessmentMemberJpaEntity jpa) {
        return new AssessmentMember(
            jpa.getId(),
            jpa.getAssessmentId(),
            jpa.getUserId(),
            roleFromString(jpa.getRole()),
            jpa.getGrantedAt(),
            jpa.getGrantedBy()
        );
    }

    public static AssessmentMemberJpaEntity toJpa(AssessmentMember domain) {
        return new AssessmentMemberJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getUserId(),
            domain.getRole().name(),
            domain.getGrantedAt(),
            domain.getGrantedBy()
        );
    }

    private static AssessmentMemberRole roleFromString(String role) {
        return role == null ? null : AssessmentMemberRole.valueOf(role);
    }
}
