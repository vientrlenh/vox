package com.sep.vox.domain.model.importfile;

import java.time.Instant;
import java.util.UUID;

public class ImportSession {
    private UUID id;
    private UUID schoolId;
    private ImportType type;
    private String fileName;
    private String originalHeadersJson;
    private String suggestedMappingJson;
    private String confirmedMappingJson;
    private long validRows;
    private long invalidRows;
    private long importedRows;
    private long skippedRows;
    private long totalRows;
    private String failureReason;
    private ImportSessionStatus status;
    private UUID importedEntityId;
    private Instant expiresAt;
    private Instant claimedAt;
    private UUID claimedBy;
    private Instant leaseExpiresAt;
    private int attempts;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ImportSession() {}

    public ImportSession(UUID id, UUID schoolId, ImportType type, String fileName, String originalHeadersJson,
            String suggestedMappingJson, String confirmedMappingJson, long validRows, long invalidRows,
            long importedRows, long skippedRows, long totalRows, String failureReason, ImportSessionStatus status,
            UUID importedEntityId, Instant expiresAt, Instant claimedAt, UUID claimedBy,
            Instant leaseExpiresAt, int attempts, Instant createdAt, Instant updatedAt,
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

    public ImportSession(UUID schoolId, ImportType type, String fileName, String originalHeadersJson,
            String suggestedMappingJson, String confirmedMappingJson, long validRows, long invalidRows,
            long importedRows, long skippedRows, long totalRows, String failureReason, ImportSessionStatus status,
            UUID importedEntityId, Instant expiresAt, Instant claimedAt, UUID claimedBy,
            Instant leaseExpiresAt, int attempts, Instant createdAt, Instant updatedAt,
            UUID createdBy, UUID updatedBy) {
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

    public ImportType getType() {
        return type;
    }

    public void setType(ImportType type) {
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

    public ImportSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ImportSessionStatus status) {
        this.status = status;
    }

    public UUID getImportedEntityId() {
        return importedEntityId;
    }

    public void setImportedEntityId(UUID importedEntityId) {
        this.importedEntityId = importedEntityId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public UUID getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(UUID claimedBy) {
        this.claimedBy = claimedBy;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
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
