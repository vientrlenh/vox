package com.sep.vox.domain.model.school;

import java.util.UUID;
import com.sep.vox.domain.model.BaseModel;
import com.sep.vox.domain.valueobject.ClassCode;

public class SchoolClass extends BaseModel {
    private UUID schoolId;
    private UUID languageId;
    private UUID schoolGradeId;
    private ClassCode code;
    private String name;
    private String description;
    private SchoolClassStatus status;

    public SchoolClass() {
    }

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

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public ClassCode getCode() {
        return code;
    }

    public void setCode(ClassCode code) {
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

    public SchoolClassStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolClassStatus status) {
        this.status = status;
    }

    public static SchoolClass create(UUID schoolId, UUID languageId, UUID schoolGradeId, String code, String name,
            String description) {
        return builder()
            .schoolId(schoolId)
            .languageId(languageId)
            .schoolGradeId(schoolGradeId)
            .code(new ClassCode(code))
            .name(name)
            .description(description)
            .status(SchoolClassStatus.ACTIVE)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<SchoolClass, Builder> {

        private Builder() {
            super(new SchoolClass());
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

        public Builder schoolGradeId(UUID schoolGradeId) {
            target.schoolGradeId = schoolGradeId;
            return this;
        }

        public Builder code(ClassCode code) {
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

        public Builder status(SchoolClassStatus status) {
            target.status = status;
            return this;
        }
    }
}
