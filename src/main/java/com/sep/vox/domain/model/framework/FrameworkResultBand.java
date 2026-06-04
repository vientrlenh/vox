package com.sep.vox.domain.model.framework;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class FrameworkResultBand {
    private UUID id;
    private UUID frameworkVersionId;
    private String code;
    private String label;
    private String description;
    private BigDecimal scoreMin; // điểm của trường -> có thể dùng cho quy đổi ra điểm của khung
    private BigDecimal scoreMax;
    private int order;
    private FrameworkResultBandStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public FrameworkResultBand() {}

    public FrameworkResultBand(UUID id, UUID frameworkVersionId, String code, String label, String description,
            BigDecimal scoreMin, BigDecimal scoreMax, int order, FrameworkResultBandStatus status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.frameworkVersionId = frameworkVersionId;
        this.code = code;
        this.label = label;
        this.description = description;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.order = order;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public FrameworkResultBand(UUID frameworkVersionId, String code, String label, String description,
            BigDecimal scoreMin, BigDecimal scoreMax, int order, FrameworkResultBandStatus status, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.frameworkVersionId = frameworkVersionId;
        this.code = code;
        this.label = label;
        this.description = description;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.order = order;
        this.status = status;
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

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getScoreMin() {
        return scoreMin;
    }

    public void setScoreMin(BigDecimal scoreMin) {
        this.scoreMin = scoreMin;
    }

    public BigDecimal getScoreMax() {
        return scoreMax;
    }

    public void setScoreMax(BigDecimal scoreMax) {
        this.scoreMax = scoreMax;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public FrameworkResultBandStatus getStatus() {
        return status;
    }

    public void setStatus(FrameworkResultBandStatus status) {
        this.status = status;
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
