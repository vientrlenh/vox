package com.sep.vox.domain.model.importfile;

import java.time.Instant;
import java.util.UUID;
import com.sep.vox.domain.model.BaseModel;

public class ImportSession extends BaseModel {
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

    public ImportSession() {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<ImportSession, Builder> {

        private Builder() {
            super(new ImportSession());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder type(ImportType type) {
            target.type = type;
            return this;
        }

        public Builder fileName(String fileName) {
            target.fileName = fileName;
            return this;
        }

        public Builder originalHeadersJson(String originalHeadersJson) {
            target.originalHeadersJson = originalHeadersJson;
            return this;
        }

        public Builder suggestedMappingJson(String suggestedMappingJson) {
            target.suggestedMappingJson = suggestedMappingJson;
            return this;
        }

        public Builder confirmedMappingJson(String confirmedMappingJson) {
            target.confirmedMappingJson = confirmedMappingJson;
            return this;
        }

        public Builder validRows(long validRows) {
            target.validRows = validRows;
            return this;
        }

        public Builder invalidRows(long invalidRows) {
            target.invalidRows = invalidRows;
            return this;
        }

        public Builder importedRows(long importedRows) {
            target.importedRows = importedRows;
            return this;
        }

        public Builder skippedRows(long skippedRows) {
            target.skippedRows = skippedRows;
            return this;
        }

        public Builder totalRows(long totalRows) {
            target.totalRows = totalRows;
            return this;
        }

        public Builder failureReason(String failureReason) {
            target.failureReason = failureReason;
            return this;
        }

        public Builder status(ImportSessionStatus status) {
            target.status = status;
            return this;
        }

        public Builder importedEntityId(UUID importedEntityId) {
            target.importedEntityId = importedEntityId;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            target.expiresAt = expiresAt;
            return this;
        }

        public Builder claimedAt(Instant claimedAt) {
            target.claimedAt = claimedAt;
            return this;
        }

        public Builder claimedBy(UUID claimedBy) {
            target.claimedBy = claimedBy;
            return this;
        }

        public Builder leaseExpiresAt(Instant leaseExpiresAt) {
            target.leaseExpiresAt = leaseExpiresAt;
            return this;
        }

        public Builder attempts(int attempts) {
            target.attempts = attempts;
            return this;
        }
    }
}
