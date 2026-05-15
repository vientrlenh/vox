package com.sep.vox.domain.model.role;

import java.time.OffsetDateTime;

import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.domain.valueobject.id.UserId;

public class Role {
    private RoleId id;
    private String code;
    private String name;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UserId createdBy;
    private UserId updatedBy;


    public Role() {
    }

    public Role(RoleId id, String code, String name, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UserId createdBy, UserId updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    // Getter and setter
    public RoleId getId() {
        return id;
    }
    public void setId(RoleId id) {
        this.id = id;
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
    public UserId getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(UserId createdBy) {
        this.createdBy = createdBy;
    }
    public UserId getUpdatedBy() {
        return updatedBy;
    }
    public void setUpdatedBy(UserId updatedBy) {
        this.updatedBy = updatedBy;
    }

    
}
