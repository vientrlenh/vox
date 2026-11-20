package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record SchoolClassUserDto(
    UUID id,
    UUID userId,
    UUID schoolClassId,
    boolean isActive,
    Instant joinedAt,
    Instant leftAt,
    UUID assignedBy
) {

}
