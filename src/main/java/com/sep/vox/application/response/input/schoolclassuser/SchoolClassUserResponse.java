package com.sep.vox.application.response.input.schoolclassuser;

import java.util.UUID;

import com.sep.vox.domain.dto.UserDto;

public record SchoolClassUserResponse(
    UUID id,
    UUID userId,
    UUID schoolClassId,
    boolean isActive,
    String joinedAt,
    String leftAt,
    UUID assignedBy,
    UserDto user
) {
}
