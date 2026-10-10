package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;

public class SchoolRoom extends BaseModel {
    private UUID schoolId;
    private Code code;
    private Name name;
    private String description;
    private SchoolRoomStatus status;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected SchoolRoom() {}

    public SchoolRoom(UUID schoolId, Code code, Name name, String description, SchoolRoomStatus status, 
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolId = schoolId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolRoom(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolId = builder.schoolId;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public SchoolRoomStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolRoomStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new SchoolRoom.Builder();
    }


    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private Code code;
        private Name name;
        private String description;
        private SchoolRoomStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder name(Name name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(SchoolRoomStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public SchoolRoom build() {
            return new SchoolRoom(this);
        }
    }
    
}
