package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.domain.valueobject.id.UserId;
import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

public class UserRoleMapper {
    
    public static UserRole toDomain(UserRoleJpaEntity jpa) {
        return new UserRole(
            jpa.getId(), 
            new UserId(jpa.getUserId()), 
            new RoleId(jpa.getRoleId()), 
            jpa.getCreatedAt()
        );
    }

    public static UserRoleJpaEntity toJpa(UserRole userRole) {
        return new UserRoleJpaEntity(
            userRole.getId(), 
            userRole.getUserId().value(), 
            userRole.getRoleId().value(), 
            userRole.getCreatedAt()
        );
    }
}
