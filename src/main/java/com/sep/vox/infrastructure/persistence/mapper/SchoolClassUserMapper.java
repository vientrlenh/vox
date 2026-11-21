package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassUserJpaEntity;

public final class SchoolClassUserMapper {
    
    public static SchoolClassUser toDomain(SchoolClassUserJpaEntity jpa) {
        return SchoolClassUser.builder()
            .id(jpa.getId())
            .userId(jpa.getUserId())
            .schoolClassId(jpa.getSchoolClassId())
            .active(jpa.isActive())
            .joinedAt(jpa.getJoinedAt())
            .leftAt(jpa.getLeftAt())
            .assignedBy(jpa.getAssignedBy())
            .build();
    }

    public static SchoolClassUserJpaEntity toJpa(SchoolClassUser schoolClassUser) {
        return SchoolClassUserJpaEntity.builder()
            .id(schoolClassUser.getId())
            .userId(schoolClassUser.getUserId())
            .schoolClassId(schoolClassUser.getSchoolClassId())
            .isActive(schoolClassUser.isActive())
            .joinedAt(schoolClassUser.getJoinedAt())
            .leftAt(schoolClassUser.getLeftAt())
            .assignedBy(schoolClassUser.getAssignedBy())
            .build();
    }
}
