package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_class_users", indexes = {
    @Index(columnList = "user_id, school_class_id", name = "idx_school_class_users", unique = true),
    @Index(columnList = "user_id", name = "idx_school_class_users_user_id")
})
public class SchoolClassUserJpaEntity {
    
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "school_class_id", nullable = false, updatable = false)
    private UUID schoolClassId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

    protected SchoolClassUserJpaEntity() {}

    public SchoolClassUserJpaEntity(UUID id, UUID userId, UUID schoolClassId, boolean isActive, OffsetDateTime joinedAt,
            OffsetDateTime leftAt, UUID assignedBy) {
        this.id = id;
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.isActive = isActive;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.assignedBy = assignedBy;
    }

    public SchoolClassUserJpaEntity(UUID userId, UUID schoolClassId, boolean isActive, OffsetDateTime joinedAt,
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

    
}
