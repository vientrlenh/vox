package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRoleDto(
    UUID id,
    UUID userId, 
    UUID roleId, 
    Instant createdAt
) {
    
}
