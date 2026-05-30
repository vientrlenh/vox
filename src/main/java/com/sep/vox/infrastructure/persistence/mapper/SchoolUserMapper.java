package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.schooluser.SchoolUser;
import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;

public final class SchoolUserMapper {

    public static SchoolUser toDomain(SchoolUserJpaEntity jpa) {
        return new SchoolUser(
            jpa.getId(),
            jpa.getUserId(),
            jpa.getSchoolId(),
            jpa.getStudentId(),
            jpa.getCreatedAt(),
            jpa.getCreatedBy()
        );
    }

    public static SchoolUserJpaEntity toJpa(SchoolUser schoolUser) {
        return new SchoolUserJpaEntity(
            schoolUser.getId(),
            schoolUser.getUserId(),
            schoolUser.getSchoolId(),
            schoolUser.getStudentId(),
            schoolUser.getCreatedAt(),
            schoolUser.getCreatedBy()
        );
    }
}
