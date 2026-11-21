package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

public class SchoolUser {
    private UUID id;
    private UUID schoolId;
    private UUID userId;
    private Instant startDate;
    private Instant endDate;

    public SchoolUser() {
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
        return builder()
            .schoolId(schoolId)
            .userId(userId)
            .startDate(now)
            .endDate(endDate)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SchoolUser target = new SchoolUser();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder startDate(Instant startDate) {
            target.startDate = startDate;
            return this;
        }

        public Builder endDate(Instant endDate) {
            target.endDate = endDate;
            return this;
        }

        public SchoolUser build() {
            return target;
        }
    }
}
