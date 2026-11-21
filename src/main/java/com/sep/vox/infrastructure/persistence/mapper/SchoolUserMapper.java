package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolUserJpaEntity;

public final class SchoolUserMapper {
    
    public static SchoolUser toDomain(SchoolUserJpaEntity jpa) {
        return SchoolUser.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .userId(jpa.getUserId())
            .startDate(jpa.getStartDate())
            .endDate(jpa.getEndDate())
            .build();
    }

    public static SchoolUserJpaEntity toJpa(SchoolUser schoolUser) {
        return SchoolUserJpaEntity.builder()
            .id(schoolUser.getId())
            .schoolId(schoolUser.getSchoolId())
            .userId(schoolUser.getUserId())
            .startDate(schoolUser.getStartDate())
            .endDate(schoolUser.getEndDate())
            .build();
    }
}
