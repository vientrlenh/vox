package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "standard_level_versions", indexes = {
    @Index(columnList = "standard_level_id, version", name = "idx_standard_level_versions_level_version", unique = true)
}, check = {
    @CheckConstraint(
        name = "chk_standard_level_version_difficulty_min_not_greater_than_difficulty_max", 
        constraint = "difficulty_min <= difficulty_max"
    ), 
    @CheckConstraint(
        name = "chk_standard_level_versions_effective_to_is_null_or_greater_than_effective_from", 
        constraint = "effective_to IS NULL OR effective_to > effective_from"
    )
})
public class StandardLevelVersionJpaEntity {
    
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "standard_level_id", nullable = false, updatable = false)
    private UUID standardLevelId;

    @Column(name = "version", nullable = false, updatable = false, check = @CheckConstraint(
        name = "chk_version_positive",
        constraint = "version > 0"
    ))
    private int version;

    @Column(name = "name", nullable = false, updatable = false, length = 100)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(
        name = "level_order", 
        nullable = false, 
        updatable = false, 
        check = @CheckConstraint(
            name = "chk_level_order_positive",
            constraint = "level_order > 0"
        )
    )
    private int order;

    @Column(
        name = "difficulty_min", 
        nullable = false, 
        updatable = false, 
        check = {
            @CheckConstraint(
                name = "chk_difficulty_min_positive", 
                constraint = "difficulty_min >= 0.00"
            )
        },
        precision = 6, 
        scale = 2
    )
    private BigDecimal difficultyMin;

    @Column(
        name = "difficulty_max", 
        nullable = false, 
        updatable = false, 
        check = @CheckConstraint(
            name = "chk_difficulty_max_positive",
            constraint = "difficulty_max >= 0.00"
        ),
        precision = 6, 
        scale = 2
    )
    private BigDecimal difficultyMax;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_status_valid",
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected StandardLevelVersionJpaEntity() {}

    public StandardLevelVersionJpaEntity(UUID id, UUID standardLevelId, int version, String name, String description,
            int order, BigDecimal difficultyMin, BigDecimal difficultyMax, String status, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.id = id;
        this.standardLevelId = standardLevelId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.order = order;
        this.difficultyMin = difficultyMin;
        this.difficultyMax = difficultyMax;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public StandardLevelVersionJpaEntity(UUID standardLevelId, int version, String name, String description, int order,
            BigDecimal difficultyMin, BigDecimal difficultyMax, String status, OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.standardLevelId = standardLevelId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.order = order;
        this.difficultyMin = difficultyMin;
        this.difficultyMax = difficultyMax;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStandardLevelId() {
        return standardLevelId;
    }

    public void setStandardLevelId(UUID standardLevelId) {
        this.standardLevelId = standardLevelId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public BigDecimal getDifficultyMin() {
        return difficultyMin;
    }

    public void setDifficultyMin(BigDecimal difficultyMin) {
        this.difficultyMin = difficultyMin;
    }

    public BigDecimal getDifficultyMax() {
        return difficultyMax;
    }

    public void setDifficultyMax(BigDecimal difficultyMax) {
        this.difficultyMax = difficultyMax;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(OffsetDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public OffsetDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(OffsetDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    
}
