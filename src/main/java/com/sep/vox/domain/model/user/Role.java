package com.sep.vox.domain.model.user;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.RoleCode;


public class Role {
    private UUID id;
    private RoleCode code;
    private String name;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;


    public Role() {
    }

    public Role(UUID id, RoleCode code, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Role(RoleCode code, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    // Getter and setter
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public RoleCode getCode() {
        return code;
    }
    public void setCode(RoleCode code) {
        this.code = code;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
