package com.sep.vox.domain.model.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserRole {
    private long id;
    private UUID userId;
    private UUID roleId;
    private OffsetDateTime createdAt;

    public UserRole() {}

    public UserRole(long id, UUID userId, UUID roleId, OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }

    public UserRole(UUID userId, UUID roleId, OffsetDateTime createdAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
}
