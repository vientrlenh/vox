package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.model.school.SchoolRoom;


public class SchoolRoomDtoMapper {
    public static SchoolRoomFromDto toDto(SchoolRoom room) {
        if (room == null) return null;

        return new SchoolRoomFromDto(
                room.getId(),
                room.getSchoolId(),
                room.getCode(),
                room.getName(),
                room.getDescription(),
                room.getCapacity(),
                room.isActive(),
                room.getCreatedAt(),
                room.getCreatedBy(),
                room.getUpdatedAt(),
                room.getUpdatedBy()
        );
    }
}
