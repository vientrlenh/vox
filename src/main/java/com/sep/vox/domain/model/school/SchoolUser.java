package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;

public class SchoolUser extends BaseModel {
    private UUID schoolId;
    private UUID userId; 
    private Code memberCode;
    private SchoolUserRole role;
    private Instant startDate;
    private Instant endDate;

    protected SchoolUser() {}

    public SchoolUser(UUID schoolId, UUID userId, Code memberCode, SchoolUserRole role, Instant startDate, Instant endDate) {
        this.schoolId = schoolId;
        this.userId = userId;
        this.memberCode = memberCode;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public SchoolUser(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolId = builder.schoolId;
        this.userId = builder.userId;
        this.memberCode = builder.memberCode;
        this.role = builder.role;
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

    public Code getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(Code memberCode) {
        this.memberCode = memberCode;
    }

    public SchoolUserRole getRole() {
        return role;
    }

    public void setRole(SchoolUserRole role) {
        this.role = role;
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

    public static SchoolUser createTeacher(UUID userId, UUID schoolId, String memberCode, Instant startDate, Instant endDate) {
        return SchoolUser.builder()
            .userId(userId)
            .schoolId(schoolId)
            .memberCode(Code.from(memberCode))
            .role(SchoolUserRole.TEACHER)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private UUID userId; 
        private Code memberCode;
        private SchoolUserRole role;
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

        public Builder memberCode(Code memberCode) {
            this.memberCode = memberCode;
            return this;
        }

        public Builder role(SchoolUserRole role) {
            this.role = role;
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
