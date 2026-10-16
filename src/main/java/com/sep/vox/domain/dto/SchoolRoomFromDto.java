package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.school.SchoolRoomStatus;

public record SchoolRoomFromDto(
    UUID id,
    UUID schoolId,
    String name,
    String description,
    SchoolRoomStatus status,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updateBy
) {

    public static SchoolRoomFromDto toDto(SchoolRoom schoolRoom) {
        return new SchoolRoomFromDto(
            schoolRoom.getId(),
            schoolRoom.getSchoolId(),
            schoolRoom.getName(),
            schoolRoom.getDescription(),
            schoolRoom.getStatus(),
            schoolRoom.getCreatedAt(),
            schoolRoom.getCreatedBy(),
            schoolRoom.getUpdatedAt(),
            schoolRoom.getUpdatedBy()
        );
    }
}
