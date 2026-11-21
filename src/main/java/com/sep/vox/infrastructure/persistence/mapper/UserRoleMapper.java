package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.infrastructure.persistence.entity.user.UserRoleJpaEntity;

public final class UserRoleMapper {
    
    public static UserRole toDomain(UserRoleJpaEntity jpa) {
        return UserRole.builder()
            .id(jpa.getId())
            .userId(jpa.getUserId())
            .roleId(jpa.getRoleId())
            .createdAt(jpa.getCreatedAt())
            .build();
    }

    public static UserRoleJpaEntity toJpa(UserRole userRole) {
        return UserRoleJpaEntity.builder()
            .id(userRole.getId())
            .userId(userRole.getUserId())
            .roleId(userRole.getRoleId())
            .createdAt(userRole.getCreatedAt())
            .build();
    }
}
