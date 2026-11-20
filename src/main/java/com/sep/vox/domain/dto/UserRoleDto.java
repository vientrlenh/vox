package com.sep.vox.domain.dto;

import java.util.UUID;

public record UserRoleDto(
    UUID id,
    UUID userId, 
    UUID roleId, 
    String createdAt
) {
    
}
