package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;

public record GradeLevelDto(
        UUID id,
        String code,
        String name,
        String description,
        Integer order,
        GradeLevelStatus status,
        Instant createdAt,
        Instant updatedAt
) {
        public static GradeLevelDto toDto(GradeLevel level) {
                return new GradeLevelDto(
                        level.getId(), 
                        Code.valueOf(level.getCode()), 
                        level.getName(), 
                        level.getDescription(), 
                        PositiveInteger.valueOf(level.getOrder()), 
                        level.getStatus(), 
                        level.getCreatedAt(), 
                        level.getUpdatedAt()
                );
        }
}
