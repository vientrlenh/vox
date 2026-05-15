package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.role.Role;
import com.sep.vox.infrastructure.persistence.entity.RoleJpaEntity;

public class RoleMapper {
    
    public static Role toDomain(RoleJpaEntity jpa) {
        return new Role(
            jpa.getId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static RoleJpaEntity toJpa(Role role) {
        return new RoleJpaEntity(
            role.getId(), 
            role.getCode(), 
            role.getName(), 
            role.getCreatedAt(), 
            role.getUpdatedAt(), 
            role.getCreatedBy(), 
            role.getUpdatedBy()
        );
    }
}
