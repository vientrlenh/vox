package com.sep.vox.infrastructure.persistence.entity;

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
@Table(name = "import_rows", indexes = {
    @Index(columnList = "session_id, row_number", name = "idx_import_rows_session_row_number", unique = true),
    @Index(columnList = "session_id, status", name = "idx_import_rows_session_status")
})
public class ImportRowJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "row_number", nullable = false, updatable = false, check = {
        @CheckConstraint(
            name = "chk_import_rows_row_number_positive",
            constraint = "row_number > 0"
        )
    })
    private long rowNumber;

    @Column(name = "raw_data_json", nullable = false, columnDefinition = "TEXT")
    private String rawDataJson;

    @Column(name = "mapped_data_json", columnDefinition = "TEXT")
    private String mappedDataJson;

    @Column(name = "errors_json", columnDefinition = "TEXT")
    private String errorsJson;

    @Column(name = "status", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_import_rows_status_valid",
            constraint = "status IN ('PENDING', 'VALID', 'INVALID', 'IMPORTED', 'SKIPPED', 'FAILED')"
        )
    })
    private String status;

    protected ImportRowJpaEntity() {}

    public ImportRowJpaEntity(UUID id, UUID sessionId, long rowNumber, String rawDataJson, String mappedDataJson,
            String errorsJson, String status) {
        this.id = id;
        this.sessionId = sessionId;
        this.rowNumber = rowNumber;
        this.rawDataJson = rawDataJson;
        this.mappedDataJson = mappedDataJson;
        this.errorsJson = errorsJson;
        this.status = status;
    }

    public ImportRowJpaEntity(UUID sessionId, long rowNumber, String rawDataJson, String mappedDataJson,
            String errorsJson, String status) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
