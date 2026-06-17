package com.sep.vox.domain.dto;

import java.util.UUID;

public record RoleDto(
    UUID id, 
    String code, 
    String name, 
    String createdAt, 
    String updatedAt
) {

}
