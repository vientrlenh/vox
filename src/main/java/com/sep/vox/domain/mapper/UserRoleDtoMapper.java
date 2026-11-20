package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.UserRoleDto;
import com.sep.vox.domain.model.user.UserRole;

public final class UserRoleDtoMapper {
    

    public static UserRoleDto toUserRoleDto(UserRole userRole) {
        return new UserRoleDto(
            userRole.getId(), 
            userRole.getUserId(), 
            userRole.getRoleId(), 
            userRole.getCreatedAt().toString()
        );
    }

    public static List<UserRoleDto> toUserRoleDtoList(List<UserRole> userRoles) {
        return userRoles.stream()
            .map(UserRoleDtoMapper::toUserRoleDto)
            .toList();
    }

    
}
