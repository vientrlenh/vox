package com.sep.vox.domain.model.assessment;

import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class AssessmentBlueprint extends BaseModel {
    private UUID schoolId;
    private UUID languageId;
    private UUID gradeLevelId;
    private String code;
    private String name;
    private String description;
    private boolean isActive;

    public AssessmentBlueprint() {}

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getLanguageId() {
        return languageId;
    }

    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
    }

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
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

    public static AssessmentBlueprint create(
            UUID schoolId, UUID languageId, UUID gradeLevelId, String code, String name, String description) {
        return builder()
            .schoolId(schoolId)
            .languageId(languageId)
            .gradeLevelId(gradeLevelId)
            .code(code)
            .name(name)
            .description(description)
            .active(true)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentBlueprint, Builder> {

        private Builder() {
            super(new AssessmentBlueprint());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder languageId(UUID languageId) {
            target.languageId = languageId;
            return this;
        }

        public Builder gradeLevelId(UUID gradeLevelId) {
            target.gradeLevelId = gradeLevelId;
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
