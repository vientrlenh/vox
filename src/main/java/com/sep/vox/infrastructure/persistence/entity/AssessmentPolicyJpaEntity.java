package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "assessment_policies", indexes = {
    @Index(columnList = "owner_type, school_id, rubric_version_id, version", name = "idx_assessment_policies_owner_rubric_version", unique = true)
})
public class AssessmentPolicyJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "status", nullable = false, length = 20)
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

    protected AssessmentPolicyJpaEntity() {}

    public AssessmentPolicyJpaEntity(UUID id, String ownerType, UUID schoolId, UUID rubricVersionId, int version,
            String status, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.ownerType = ownerType;
        this.schoolId = schoolId;
        this.rubricVersionId = rubricVersionId;
        this.version = version;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public AssessmentPolicyJpaEntity(String ownerType, UUID schoolId, UUID rubricVersionId, int version, String status,
            OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.ownerType = ownerType;
        this.schoolId = schoolId;
        this.rubricVersionId = rubricVersionId;
        this.version = version;
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

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
