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

    public ImportRow() {}

    public ImportRow(UUID id, UUID sessionId, long rowNumber, String rawDataJson, String mappedDataJson,
            String errorsJson, ImportRowStatus status) {
        this.id = id;
        this.sessionId = sessionId;
        this.rowNumber = rowNumber;
        this.rawDataJson = rawDataJson;
        this.mappedDataJson = mappedDataJson;
        this.errorsJson = errorsJson;
        this.status = status;
    }

    public ImportRow(UUID sessionId, long rowNumber, String rawDataJson, String mappedDataJson, String errorsJson,
            ImportRowStatus status) {
        this.sessionId = sessionId;
        this.rowNumber = rowNumber;
        this.rawDataJson = rawDataJson;
        this.mappedDataJson = mappedDataJson;
        this.errorsJson = errorsJson;
        this.status = status;
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

    
}
