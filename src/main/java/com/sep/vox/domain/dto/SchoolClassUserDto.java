package com.sep.vox.domain.dto;

import java.util.UUID;

public record SchoolClassUserDto(
    UUID id,
    UUID userId,
    UUID schoolClassId,
    boolean isActive,
    String joinedAt,
    String leftAt,
    UUID assignedBy
) {

}
