package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record RoleDto(
    UUID id, 
    String code, 
    String name, 
    Instant createdAt, 
    Instant updatedAt
) {

}
