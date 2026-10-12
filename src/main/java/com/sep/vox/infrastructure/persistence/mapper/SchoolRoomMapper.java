package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.school.SchoolRoomStatus;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolRoomJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class SchoolRoomMapper {

    private SchoolRoomMapper() {}

    public static SchoolRoom toDomain(SchoolRoomJpaEntity jpa) {
        try {
            return SchoolRoom.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .status(SchoolRoomStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("A problem occurred when mapping school room entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("An error occurred when converting entity to model");
        }

    }

    public static SchoolRoomJpaEntity toJpa(SchoolRoom room) {
        try {
            return SchoolRoomJpaEntity.builder()
                .id(room.getId())
                .schoolId(room.getSchoolId())
                .name(room.getName())
                .description(room.getDescription())
                .status(SchoolRoomStatus.value(room.getStatus()))
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .createdBy(room.getCreatedBy())
                .updatedBy(room.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("A problem occurred when mapping school room model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("An error occurred when converting model to entity");
        }
    }
}
