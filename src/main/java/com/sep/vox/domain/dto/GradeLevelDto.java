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
                String status,
                String createdAt,
                String updatedAt) {
        public static GradeLevelDto toDto(GradeLevel level) {
                return new GradeLevelDto(
                                level.getId(),
                                level.getCode(),
                                level.getName(), 
                                level.getDescription(), 
                                level.getOrder(), 
                                valueOf(level.getStatus()),
                                toIso(level.getCreatedAt()),
                                toIso(level.getUpdatedAt())
                );
        }

        private static String valueOf(GradeLevelStatus status) {
                return status == null ? null : status.name();
        }

        // createdAt/updatedAt có thể null với entity chưa flush -- .toString() trực tiếp sẽ NPE.
        private static String toIso(Instant instant) {
                return instant == null ? null : instant.toString();
        }
}
