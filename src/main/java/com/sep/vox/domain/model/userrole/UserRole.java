package com.sep.vox.domain.model.userrole;

import java.time.OffsetDateTime;

import com.sep.vox.domain.valueobject.id.RoleId;
import com.sep.vox.domain.valueobject.id.UserId;

public class UserRole {
    private long id;
    private UserId userId;
    private RoleId roleId;
    private OffsetDateTime createdAt;

    public UserRole() {}

    public UserRole(long id, UserId userId, RoleId roleId, OffsetDateTime createdAt) {
        this.id = id;
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

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public RoleId getRoleId() {
        return roleId;
    }

    public void setRoleId(RoleId roleId) {
        this.roleId = roleId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
}
