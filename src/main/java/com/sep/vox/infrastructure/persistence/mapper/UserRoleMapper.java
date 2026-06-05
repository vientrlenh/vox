package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

public final class UserRoleMapper {
    
    public static UserRole toDomain(UserRoleJpaEntity jpa) {
        return new UserRole(
            jpa.getId(), 
            jpa.getUserId(), 
            jpa.getRoleId(), 
            jpa.getCreatedAt()
        );
    }

    public static UserRoleJpaEntity toJpa(UserRole userRole) {
        return new UserRoleJpaEntity(
            userRole.getId(), 
            userRole.getUserId(), 
            userRole.getRoleId(), 
            userRole.getCreatedAt()
        );
    }
}
