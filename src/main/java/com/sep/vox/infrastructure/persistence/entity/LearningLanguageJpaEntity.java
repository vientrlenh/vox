package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity 
@Table(name = "learning_languages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter 
@SuperBuilder 
public class LearningLanguageJpaEntity extends BaseEntity {
    
    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_learning_languages_status_valid", 
            constraint = "status IN ('AVAILABLE', 'UNAVAILABLE')"
        )
    })
    private String status;

    @Column(name = "available_at")
    private Instant availableAt;

    @Column(name = "unavailable_at")
    private Instant unavailableAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
