package com.sep.vox.domain.model.school;

import java.time.LocalDate;
import java.util.UUID;
import com.sep.vox.domain.model.BaseModel;

public class SchoolGrade extends BaseModel {
    private UUID schoolId;
    private UUID gradeLevelId;
    private String code;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private SchoolGradeStatus status;

    public SchoolGrade() {
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public SchoolGradeStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolGradeStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<SchoolGrade, Builder> {

        private Builder() {
            super(new SchoolGrade());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
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

        public Builder startDate(LocalDate startDate) {
            target.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            target.endDate = endDate;
            return this;
        }

        public Builder status(SchoolGradeStatus status) {
            target.status = status;
            return this;
        }
    }
}
