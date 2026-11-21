package com.sep.vox.domain.model.school;

import java.util.UUID;
import com.sep.vox.domain.model.BaseModel;

public class SchoolRoom extends BaseModel {
    private UUID schoolId;
    private String code;
    private String name;
    private String description;
    private boolean isActive;

    public SchoolRoom() {
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<SchoolRoom, Builder> {

        private Builder() {
            super(new SchoolRoom());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder active(boolean isActive) {
            target.isActive = isActive;
            return this;
        }
    }
}
