package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

public class SchoolUser {
    private UUID id;
    private UUID schoolId;
    private UUID userId; 
    private Instant startDate;
    private Instant endDate;

    
    public SchoolUser() {}

    public SchoolUser(UUID id, UUID schoolId, UUID userId, Instant startDate, Instant endDate) {
        this.id = id;
        this.schoolId = schoolId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public SchoolUser(UUID schoolId, UUID userId, Instant startDate, Instant endDate) {
        this.schoolId = schoolId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public static SchoolUser create(UUID userId, UUID schoolId, Instant now, Instant endDate) {
        return new SchoolUser(
            schoolId, 
            userId, 
            now, 
            endDate
        );
    }
}
