package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.valueobject.RoleCode;
import com.sep.vox.infrastructure.persistence.entity.user.RoleJpaEntity;

public final class RoleMapper {
    
    public static Role toDomain(RoleJpaEntity jpa) {
        return Role.builder()
            .id(jpa.getId())
            .code(new RoleCode(jpa.getCode()))
            .name(jpa.getName())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static RoleJpaEntity toJpa(Role role) {
        return RoleJpaEntity.builder()
            .id(role.getId())
            .code(valueOf(role.getCode()))
            .name(role.getName())
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .createdBy(role.getCreatedBy())
            .updatedBy(role.getUpdatedBy())
            .build();
    }

    private static String valueOf(RoleCode roleCode) {
        return roleCode == null ? null : roleCode.value();
    }
}
