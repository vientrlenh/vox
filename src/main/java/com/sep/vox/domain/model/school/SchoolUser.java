package com.sep.vox.domain.model.school;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SchoolUser {
    private UUID id;
    private UUID schoolId;
    private UUID userId; 
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    
    public SchoolUser() {}

    public SchoolUser(UUID id, UUID schoolId, UUID userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        this.id = id;
        this.schoolId = schoolId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public SchoolUser(UUID schoolId, UUID userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        this.schoolId = schoolId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static SchoolUser create(String studentId, UUID schoolId, UUID userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return new SchoolUser(studentId, schoolId, userId, startDate, endDate);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }


    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public static SchoolUser create(UUID userId, UUID schoolId, OffsetDateTime now, OffsetDateTime endDate) {
        return new SchoolUser(
            schoolId, 
            userId, 
            now, 
            endDate
        );
    }
}
