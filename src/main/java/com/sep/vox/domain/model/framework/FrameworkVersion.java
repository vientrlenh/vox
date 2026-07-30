package com.sep.vox.domain.model.framework;

import java.time.Instant;
import java.util.UUID;

public class FrameworkVersion {
    private UUID id;
    private UUID frameworkId;
    private String code;
    private String name;
    private String description;
    private int version;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private FrameworkVersionStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public FrameworkVersion() {}

    public FrameworkVersion(UUID id, UUID frameworkId, String code, String name, String description, int version,
            Instant effectiveFrom, Instant effectiveTo, FrameworkVersionStatus status, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.frameworkId = frameworkId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public FrameworkVersion(UUID frameworkId, String code, String name, String description, int version,
            Instant effectiveFrom, Instant effectiveTo, FrameworkVersionStatus status, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.frameworkId = frameworkId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
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

    public UUID getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(UUID frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
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

    public FrameworkVersionStatus getStatus() {
        return status;
    }

    public void setStatus(FrameworkVersionStatus status) {
        this.status = status;
    }

}
