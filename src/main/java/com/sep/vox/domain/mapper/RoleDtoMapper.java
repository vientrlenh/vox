package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.model.user.Role;

public final class RoleDtoMapper {
    
    public static RoleDto toRoleDto(Role role) {
        return new RoleDto(
            role.getId(), 
            role.getCode().value(), 
            role.getName(), 
            role.getCreatedAt().toString(), 
            role.getUpdatedAt().toString()
        );
    }

    public static List<RoleDto> toRoleDtoList(List<Role> roles) {
        return roles.stream()
            .map(RoleDtoMapper::toRoleDto)
            .toList();
    }

    public static PageResult<RoleDto> toRoleDtoPage(PageResult<Role> rolePage) {
        return new PageResult<>(
            toRoleDtoList(rolePage.content()), 
            rolePage.page(), 
            rolePage.size(), 
            rolePage.totalElements(), 
            rolePage.totalPages()
        );
    }
}
