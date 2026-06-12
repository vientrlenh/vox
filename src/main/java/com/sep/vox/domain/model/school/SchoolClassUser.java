package com.sep.vox.domain.model.school;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SchoolClassUser {
    private UUID id;
    private UUID userId;
    private UUID schoolClassId;
    private boolean isActive;
    private OffsetDateTime joinedAt;
    private OffsetDateTime leftAt;
    private UUID assignedBy;

    public SchoolClassUser() {}

    public SchoolClassUser(UUID id, UUID userId, UUID schoolClassId, boolean isActive, OffsetDateTime joinedAt,
            OffsetDateTime leftAt, UUID assignedBy) {
        this.id = id;
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.isActive = isActive;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.assignedBy = assignedBy;
    }

    public SchoolClassUser(UUID userId, UUID schoolClassId, boolean isActive, OffsetDateTime joinedAt,
            OffsetDateTime leftAt, UUID assignedBy) {
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.isActive = isActive;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.assignedBy = assignedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(UUID schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public OffsetDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(OffsetDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public void deactivate(OffsetDateTime leftAt) {
        this.isActive = false;
        this.leftAt = leftAt;
    }

    public void activate() {
        this.isActive = true;
        this.leftAt = null;
    }

    
}
