package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;

public record GradeLevelDto(
                UUID id,
                String code,
                String name,
                String description,
                int order,
                GradeLevelStatus status,
                Instant createdAt,
                Instant updatedAt) {
        public static GradeLevelDto toDto(GradeLevel level) {
                return new GradeLevelDto(
                                level.getId(),
                                level.getCode(),
                                level.getName(), 
                                level.getDescription(), 
                                level.getOrder(), 
                                level.getStatus(),
                                level.getCreatedAt(),
                                level.getUpdatedAt()
                );
        }
}
