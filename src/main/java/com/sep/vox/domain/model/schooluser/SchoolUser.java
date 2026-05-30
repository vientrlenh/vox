package com.sep.vox.domain.model.schooluser;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SchoolUser {
    private long id;
    private UUID userId;
    private UUID schoolId;
    private String studentId;
    private OffsetDateTime createdAt;
    private UUID createdBy;

    public SchoolUser() {}

    public SchoolUser(long id, UUID userId, UUID schoolId, String studentId, OffsetDateTime createdAt, UUID createdBy) {
        this.id = id;
        this.userId = userId;
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public SchoolUser(UUID userId, UUID schoolId, String studentId, OffsetDateTime createdAt, UUID createdBy) {
        this.userId = userId;
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static SchoolUser create(UUID userId, UUID schoolId, String studentId, UUID createdBy, OffsetDateTime now) {
        return new SchoolUser(userId, schoolId, studentId, now, createdBy);
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

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
