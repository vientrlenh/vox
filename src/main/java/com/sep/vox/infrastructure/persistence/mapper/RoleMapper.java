package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.domain.valueobject.id.UserId;
import com.sep.vox.infrastructure.persistence.entity.RoleJpaEntity;

public class RoleMapper {
    
    public static Role toDomain(RoleJpaEntity jpa) {
        return new Role(
            new RoleId(jpa.getId()),
            jpa.getCode(),
            jpa.getName(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            new UserId(jpa.getCreatedBy()),
            new UserId(jpa.getUpdatedBy())
        );
    }

    public static RoleJpaEntity toJpa(Role role) {
        return new RoleJpaEntity(
            role.getId().value(), 
            role.getCode(), 
            role.getName(), 
            role.getCreatedAt(), 
            role.getUpdatedAt(), 
            role.getCreatedBy().value(), 
            role.getUpdatedBy().value()
        );
    }
}
