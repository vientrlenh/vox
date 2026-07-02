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
@Table(name = "exam_secure_pools")
public class ExamSecurePoolJpaEntity {

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

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_secure_pools_status_valid",
            constraint = "status IN ('SEALED', 'RELEASED')"
        )
    })
    private String status;

    @Column(name = "release_mode", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_secure_pools_release_mode_valid",
            constraint = "release_mode IN ('MANUAL', 'AUTO_AFTER_CLOSE')"
        )
    })
    private String releaseMode;

    @Column(name = "embargo_until")
    private OffsetDateTime embargoUntil;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ExamSecurePoolJpaEntity() {}

    public ExamSecurePoolJpaEntity(UUID id, UUID examId, String status, String releaseMode,
            OffsetDateTime embargoUntil, OffsetDateTime releasedAt, UUID releasedBy, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.examId = examId;
        this.status = status;
        this.releaseMode = releaseMode;
        this.embargoUntil = embargoUntil;
        this.releasedAt = releasedAt;
        this.releasedBy = releasedBy;
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

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReleaseMode() {
        return releaseMode;
    }

    public void setReleaseMode(String releaseMode) {
        this.releaseMode = releaseMode;
    }

    public OffsetDateTime getEmbargoUntil() {
        return embargoUntil;
    }

    public void setEmbargoUntil(OffsetDateTime embargoUntil) {
        this.embargoUntil = embargoUntil;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(OffsetDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public UUID getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(UUID releasedBy) {
        this.releasedBy = releasedBy;
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
