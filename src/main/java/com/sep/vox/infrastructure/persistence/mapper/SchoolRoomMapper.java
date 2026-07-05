package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;

public final class SchoolRoomMapper {


    public static SchoolRoom toDomain(SchoolRoomJpaEntity jpa) {
        return new SchoolRoom(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getCapacity(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static SchoolRoomJpaEntity toJpa(SchoolRoom schoolRoom) {
        return new SchoolRoomJpaEntity(
            schoolRoom.getId(),
            schoolRoom.getSchoolId(),
            schoolRoom.getCode(),
            schoolRoom.getName(),
            schoolRoom.getDescription(),
            schoolRoom.getCapacity(),
            schoolRoom.isActive(),
            schoolRoom.getCreatedAt(),
            schoolRoom.getUpdatedAt(),
            schoolRoom.getCreatedBy(),
            schoolRoom.getUpdatedBy()
        );
    }
}
