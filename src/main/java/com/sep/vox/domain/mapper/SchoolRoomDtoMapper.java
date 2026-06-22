package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.model.school.SchoolRoom;

import java.util.List;

public class SchoolRoomDtoMapper {
    public static SchoolRoomFromDto toDto(SchoolRoom room) {
        if (room == null) return null;

        return new SchoolRoomFromDto(
                room.getId(),
                room.getSchoolId(),
                room.getCode(),
                room.getName(),
                room.getDescription(),
                room.isActive(),
                room.getCreatedAt(),
                room.getCreatedBy(),
                room.getUpdatedAt(),
                room.getUpdatedBy()
        );
    }
}
