package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Xem V42__grade_level_band_scopes.sql. Trần lưu bằng ID bậc chứ không bằng result_band_order --
 * nên ở đây không còn check constraint "default <= hard_max": so sánh thứ tự phải join sang
 * framework_result_bands, việc đó do GradeLevelBandScopeGuardService làm.
 */
@Entity
@Table(name = "grade_level_band_scopes", indexes = {
    @Index(columnList = "grade_level_id, framework_version_id", name = "idx_grade_level_band_scopes_level_framework", unique = true),
    @Index(columnList = "default_target_band_id", name = "idx_grade_level_band_scopes_default_band"),
    @Index(columnList = "hard_max_band_id", name = "idx_grade_level_band_scopes_hard_max_band")
})
public class GradeLevelBandScopeJpaEntity {

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

    @Column(name = "grade_level_id", nullable = false, updatable = false)
    private UUID gradeLevelId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "default_target_band_id", nullable = false)
    private UUID defaultTargetBandId;

    @Column(name = "hard_max_band_id", nullable = false)
    private UUID hardMaxBandId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected GradeLevelBandScopeJpaEntity() {}

    public GradeLevelBandScopeJpaEntity(UUID id, UUID gradeLevelId, UUID frameworkVersionId, UUID defaultTargetBandId,
            UUID hardMaxBandId, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.gradeLevelId = gradeLevelId;
        this.frameworkVersionId = frameworkVersionId;
        this.defaultTargetBandId = defaultTargetBandId;
        this.hardMaxBandId = hardMaxBandId;
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

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public UUID getDefaultTargetBandId() {
        return defaultTargetBandId;
    }

    public void setDefaultTargetBandId(UUID defaultTargetBandId) {
        this.defaultTargetBandId = defaultTargetBandId;
    }

    public UUID getHardMaxBandId() {
        return hardMaxBandId;
    }

    public void setHardMaxBandId(UUID hardMaxBandId) {
        this.hardMaxBandId = hardMaxBandId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
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
