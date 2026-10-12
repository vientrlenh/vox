package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassUserJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class SchoolClassUserMapper {

    private SchoolClassUserMapper() {}
    
    public static SchoolClassUser toDomain(SchoolClassUserJpaEntity jpa) {
        try {
            return SchoolClassUser.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .schoolClassId(jpa.getSchoolClassId())
                .joinAt(jpa.getJoinAt())
                .leftAt(jpa.getLeftAt())
                .assignedBy(jpa.getAssignedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school class user entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static SchoolClassUserJpaEntity toJpa(SchoolClassUser user) {
        try {
            return SchoolClassUserJpaEntity.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .schoolClassId(user.getSchoolClassId())
                .joinAt(user.getJoinAt())
                .leftAt(user.getLeftAt())
                .assignedBy(user.getAssignedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school class user model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }

    }
}
