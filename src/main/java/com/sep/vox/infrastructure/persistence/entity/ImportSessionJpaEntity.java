package com.sep.vox.infrastructure.persistence.entity;

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
@Table(name = "import_sessions", indexes = {
    @Index(columnList = "school_id, type, status", name = "idx_import_sessions_school_type_status"),
    @Index(columnList = "school_id, created_at", name = "idx_import_sessions_school_created_at"),
    @Index(columnList = "expires_at", name = "idx_import_sessions_expires_at")
}, check = {
    @CheckConstraint(
        name = "chk_import_sessions_row_counts_non_negative",
        constraint = "valid_rows >= 0 AND invalid_rows >= 0 AND imported_rows >= 0 AND skipped_rows >= 0 AND total_rows >= 0"
    )
})
public class ImportSessionJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "school_id", updatable = false)
    private UUID schoolId;

    @Column(name = "type", nullable = false, updatable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_import_sessions_type_valid",
            constraint = "type IN ('USER', 'SCHOOL_CLASS', 'SCHOOL_CLASS_USER','QUESTION', 'SCHOOL_DIRECTORY', 'SCHOOL_GRADE_LEVEL', 'SCHOOL_GRADE','RUBRIC_VERSION','RUBRIC_CRITERION','RUBRIC_CRITERION_BAND')"
        )
    })
    private String type;

    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "original_headers_json", nullable = false, columnDefinition = "TEXT")
    private String originalHeadersJson;

    @Column(name = "suggested_mapping_json", columnDefinition = "TEXT")
    private String suggestedMappingJson;

    @Column(name = "confirmed_mapping_json", columnDefinition = "TEXT")
    private String confirmedMappingJson;

    @Column(name = "valid_rows", nullable = false)
    private long validRows;

    @Column(name = "invalid_rows", nullable = false)
    private long invalidRows;

    @Column(name = "imported_rows", nullable = false)
    private long importedRows;

    @Column(name = "skipped_rows", nullable = false)
    private long skippedRows;

    @Column(name = "total_rows", nullable = false)
    private long totalRows;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "status", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_import_sessions_status_valid",
            constraint = "status IN ('PREVIEWED', 'VALIDATING', 'IMPORTING', 'QUEUED', 'COMPLETED', 'FAILED', 'EXPIRED', 'CANCELLED')"
        ),
    })
    private String status;

    @Column(name = "imported_entity_id")
    private UUID importedEntityId;

    @Column(name = "expires_at", nullable = false) 
    private OffsetDateTime expiresAt;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "claimed_by")
    private UUID claimedBy; 

    @Column(name = "lease_expires_at")
    private OffsetDateTime leaseExpiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    protected ImportSessionJpaEntity() {}

    public ImportSessionJpaEntity(UUID id, UUID schoolId, String type, String fileName, String originalHeadersJson,
            String suggestedMappingJson, String confirmedMappingJson, long validRows, long invalidRows,
            long importedRows, long skippedRows, long totalRows, String failureReason, String status,
            UUID importedEntityId, OffsetDateTime expiresAt, OffsetDateTime claimedAt, UUID claimedBy,
            OffsetDateTime leaseExpiresAt, int attempts, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.type = type;
        this.fileName = fileName;
        this.originalHeadersJson = originalHeadersJson;
        this.suggestedMappingJson = suggestedMappingJson;
        this.confirmedMappingJson = confirmedMappingJson;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.importedRows = importedRows;
        this.skippedRows = skippedRows;
        this.totalRows = totalRows;
        this.failureReason = failureReason;
        this.status = status;
        this.importedEntityId = importedEntityId;
        this.expiresAt = expiresAt;
        this.claimedAt = claimedAt;
        this.claimedBy = claimedBy;
        this.leaseExpiresAt = leaseExpiresAt;
        this.attempts = attempts;
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

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalHeadersJson() {
        return originalHeadersJson;
    }

    public void setOriginalHeadersJson(String originalHeadersJson) {
        this.originalHeadersJson = originalHeadersJson;
    }

    public String getSuggestedMappingJson() {
        return suggestedMappingJson;
    }

    public void setSuggestedMappingJson(String suggestedMappingJson) {
        this.suggestedMappingJson = suggestedMappingJson;
    }

    public String getConfirmedMappingJson() {
        return confirmedMappingJson;
    }

    public void setConfirmedMappingJson(String confirmedMappingJson) {
        this.confirmedMappingJson = confirmedMappingJson;
    }

    public long getValidRows() {
        return validRows;
    }

    public void setValidRows(long validRows) {
        this.validRows = validRows;
    }

    public long getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(long invalidRows) {
        this.invalidRows = invalidRows;
    }

    public long getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(long importedRows) {
        this.importedRows = importedRows;
    }

    public long getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(long skippedRows) {
        this.skippedRows = skippedRows;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getImportedEntityId() {
        return importedEntityId;
    }

    public void setImportedEntityId(UUID importedEntityId) {
        this.importedEntityId = importedEntityId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    public OffsetDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(OffsetDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public UUID getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(UUID claimedBy) {
        this.claimedBy = claimedBy;
    }

    public OffsetDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(OffsetDateTime leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
