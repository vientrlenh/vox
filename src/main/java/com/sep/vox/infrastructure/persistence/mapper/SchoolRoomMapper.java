package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolRoomJpaEntity;

public final class SchoolRoomMapper {


    public static SchoolRoom toDomain(SchoolRoomJpaEntity jpa) {
        return SchoolRoom.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .description(jpa.getDescription())
            .active(jpa.isActive())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static SchoolRoomJpaEntity toJpa(SchoolRoom schoolRoom) {
        return SchoolRoomJpaEntity.builder()
            .id(schoolRoom.getId())
            .schoolId(schoolRoom.getSchoolId())
            .code(schoolRoom.getCode())
            .name(schoolRoom.getName())
            .description(schoolRoom.getDescription())
            .isActive(schoolRoom.isActive())
            .createdAt(schoolRoom.getCreatedAt())
            .updatedAt(schoolRoom.getUpdatedAt())
            .createdBy(schoolRoom.getCreatedBy())
            .updatedBy(schoolRoom.getUpdatedBy())
            .build();
    }
}
