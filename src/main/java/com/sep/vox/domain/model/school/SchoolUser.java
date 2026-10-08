package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class SchoolUser extends BaseModel {
    private UUID schoolId;
    private UUID userId; 
    private Instant startDate;
    private Instant endDate;

    protected SchoolUser() {}

    public SchoolUser(UUID schoolId, UUID userId, Instant startDate, Instant endDate) {
        this.schoolId = schoolId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public SchoolUser(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolId = builder.schoolId;
        this.userId = builder.userId;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
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

    public static Builder builder() {
        return new SchoolUser.Builder();
    }

    public static SchoolUser create(UUID userId, UUID schoolId, Instant now, Instant endDate) {
        return new SchoolUser(
            schoolId, 
            userId, 
            now, 
            endDate
        );
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private UUID userId; 
        private Instant startDate;
        private Instant endDate;
        private Instant createdAt;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SchoolUser build() {
            return new SchoolUser(this);
        }
    }
}
