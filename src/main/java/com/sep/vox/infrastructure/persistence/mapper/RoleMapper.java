package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.valueobject.RoleCode;
import com.sep.vox.infrastructure.persistence.entity.RoleJpaEntity;

public final class RoleMapper {
    
    public static Role toDomain(RoleJpaEntity jpa) {
        return new Role(
            jpa.getId(),
            new RoleCode(jpa.getCode()),
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
            valueOf(role.getCode()), 
            role.getName(), 
            role.getCreatedAt(), 
            role.getUpdatedAt(), 
            role.getCreatedBy(), 
            role.getUpdatedBy()
        );
    }

    private static String valueOf(RoleCode roleCode) {
        return roleCode == null ? null : roleCode.value();
    }
}
