package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.schoolclassuser.SchoolClassUser;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;

public final class SchoolClassUserMapper {
    
    public static SchoolClassUser toDomain(SchoolClassUserJpaEntity jpa) {
        return new SchoolClassUser(
            jpa.getId(), 
            jpa.getUserId(), 
            jpa.getSchoolClassId(), 
            jpa.isActive(),
            jpa.getJoinedAt(), 
            jpa.getLeftAt(), 
            jpa.getAssignedBy()
        );
    }

    public static SchoolClassUserJpaEntity toJpa(SchoolClassUser schoolClassUser) {
        return new SchoolClassUserJpaEntity(
            schoolClassUser.getId(), 
            schoolClassUser.getUserId(), 
            schoolClassUser.getSchoolClassId(), 
            schoolClassUser.isActive(), 
            schoolClassUser.getJoinedAt(), 
            schoolClassUser.getLeftAt(), 
            schoolClassUser.getAssignedBy()
        );
    }
}
