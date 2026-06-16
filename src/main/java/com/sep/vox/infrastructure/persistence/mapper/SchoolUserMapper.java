package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;

public final class SchoolUserMapper {
    
    public static SchoolUser toDomain(SchoolUserJpaEntity jpa) {
        return new SchoolUser(
            jpa.getId(),  
            jpa.getSchoolId(), 
            jpa.getUserId(), 
            jpa.getStartDate(), 
            jpa.getEndDate()
        );
    }

    public static SchoolUserJpaEntity toJpa(SchoolUser schoolUser) {
        return new SchoolUserJpaEntity(
            schoolUser.getId(),  
            schoolUser.getSchoolId(), 
            schoolUser.getUserId(), 
            schoolUser.getStartDate(), 
            schoolUser.getEndDate()
        );
    }
}
