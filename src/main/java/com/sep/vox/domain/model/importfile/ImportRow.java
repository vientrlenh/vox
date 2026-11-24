package com.sep.vox.domain.model.importfile;

import java.util.UUID;

public class ImportRow {
    private UUID id;
    private UUID sessionId;
    private long rowNumber;
    private String rawDataJson;
    private String mappedDataJson;
    private String errorsJson;
    private ImportRowStatus status;

    public ImportRow() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public long getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(long rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRawDataJson() {
        return rawDataJson;
    }

    public void setRawDataJson(String rawDataJson) {
        this.rawDataJson = rawDataJson;
    }

    public String getMappedDataJson() {
        return mappedDataJson;
    }

    public void setMappedDataJson(String mappedDataJson) {
        this.mappedDataJson = mappedDataJson;
    }

    public String getErrorsJson() {
        return errorsJson;
    }

    public void setErrorsJson(String errorsJson) {
        this.errorsJson = errorsJson;
    }

    public ImportRowStatus getStatus() {
        return status;
    }

    public void setStatus(ImportRowStatus status) {
        this.status = status;
    }

    public static ImportRow pending(UUID sessionId, long rowNumber, String rawDataJson) {
        return builder()
                .sessionId(sessionId)
                .rowNumber(rowNumber)
                .rawDataJson(rawDataJson)
                .status(ImportRowStatus.PENDING)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ImportRow target = new ImportRow();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder sessionId(UUID sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder rowNumber(long rowNumber) {
            target.rowNumber = rowNumber;
            return this;
        }

        public Builder rawDataJson(String rawDataJson) {
            target.rawDataJson = rawDataJson;
            return this;
        }

        public Builder mappedDataJson(String mappedDataJson) {
            target.mappedDataJson = mappedDataJson;
            return this;
        }

        public Builder errorsJson(String errorsJson) {
            target.errorsJson = errorsJson;
            return this;
        }

        public Builder status(ImportRowStatus status) {
            target.status = status;
            return this;
        }

        public ImportRow build() {
            return target;
        }
    }
}
