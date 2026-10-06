package com.sep.vox.application.projection.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRoleInfo(
    UUID id,
    UUID userId,
    UUID roleId,
    Instant createdAt,
    String roleCode,
    String roleName
) {
    
}
