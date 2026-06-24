package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.infrastructure.persistence.entity.ExamMemberJpaEntity;

public final class ExamMemberMapper {

    private ExamMemberMapper() {}

    public static ExamMember toDomain(ExamMemberJpaEntity jpa) {
        return new ExamMember(
            jpa.getId(),
            jpa.getExamId(),
            jpa.getUserId(),
            roleFromString(jpa.getRole()),
            jpa.getGrantedAt(),
            jpa.getGrantedBy()
        );
    }

    public static ExamMemberJpaEntity toJpa(ExamMember domain) {
        return new ExamMemberJpaEntity(
            domain.getId(),
            domain.getExamId(),
            domain.getUserId(),
            domain.getRole().name(),
            domain.getGrantedAt(),
            domain.getGrantedBy()
        );
    }

    private static ExamMemberRole roleFromString(String role) {
        return role == null ? null : ExamMemberRole.valueOf(role);
    }
}
