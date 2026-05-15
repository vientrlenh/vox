package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserRoleInfo(
    long id,
    UUID userId,
    UUID roleId,
    OffsetDateTime createdAt,
    String roleCode,
    String roleName
) {
    
}
