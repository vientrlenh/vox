package com.sep.vox.domain.model.rubric;

import java.util.UUID;

public class Rubric {
    private UUID id;
    private UUID languageId;
    private UUID frameworkId;
    private String code;
    private String name;
    private String description;
    private RubricOwnerType ownerType;
    private UUID schoolId;

    public Rubric() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLanguageId() {
        return languageId;
    }

    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
    }

    public UUID getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(UUID frameworkId) {
        this.frameworkId = frameworkId;
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

    public RubricOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(RubricOwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public static Rubric forSchool(
            UUID languageId, UUID frameworkId, String code, String name, String description, UUID schoolId) {
        return builder()
                .languageId(languageId)
                .frameworkId(frameworkId)
                .code(code)
                .name(name)
                .description(description)
                .ownerType(RubricOwnerType.SCHOOL)
                .schoolId(schoolId)
                .build();
    }

    public static Rubric forSystem(
            UUID languageId, UUID frameworkId, String code, String name, String description) {
        return builder()
                .languageId(languageId)
                .frameworkId(frameworkId)
                .code(code)
                .name(name)
                .description(description)
                .ownerType(RubricOwnerType.SYSTEM)
                .schoolId(null)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Rubric target = new Rubric();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder languageId(UUID languageId) {
            target.languageId = languageId;
            return this;
        }

        public Builder frameworkId(UUID frameworkId) {
            target.frameworkId = frameworkId;
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

        public Builder ownerType(RubricOwnerType ownerType) {
            target.ownerType = ownerType;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Rubric build() {
            return target;
        }
    }
}
