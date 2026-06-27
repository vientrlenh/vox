package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_blueprint_versions")
public class ExamBlueprintVersionJpaEntity {
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

    @Column(name = "blue_print_id", nullable = false, updatable = false)
    private UUID blueprintId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_blueprint_versions_status_valid", 
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "total_time_limit_seconds", check = {
        @CheckConstraint(
            name = "chk_exam_blueprint_versions_total_time_limit_seconds_valid", 
            constraint = "total_time_limit_seconds > 0"
        )
    })
    private Integer totalTimeLimitSeconds;

    @Column(name = "effective_from", nullable = false)
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

    protected ExamBlueprintVersionJpaEntity() {}

    public ExamBlueprintVersionJpaEntity(UUID id, UUID blueprintId, int version, String code, String description,
            String status, Integer totalTimeLimitSeconds, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.blueprintId = blueprintId;
        this.version = version;
        this.code = code;
        this.description = description;
        this.status = status;
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
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

    public UUID getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(UUID blueprintId) {
        this.blueprintId = blueprintId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalTimeLimitSeconds() {
        return totalTimeLimitSeconds;
    }

    public void setTotalTimeLimitSeconds(Integer totalTimeLimitSeconds) {
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
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
