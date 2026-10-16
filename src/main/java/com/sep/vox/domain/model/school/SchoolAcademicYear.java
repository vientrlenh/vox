package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class SchoolAcademicYear extends BaseModel {
    private UUID schoolId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private SchoolAcademicYearStatus status;
    private Instant activatedAt;
    private Instant closedAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected SchoolAcademicYear() {}

    public SchoolAcademicYear(UUID schoolId, String name, LocalDate startDate, LocalDate endDate,
            SchoolAcademicYearStatus status, Instant activatedAt, Instant closedAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.schoolId = schoolId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.activatedAt = activatedAt;
        this.closedAt = closedAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolAcademicYear(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolId = builder.schoolId;
        this.name = builder.name;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.status = builder.status;
        this.activatedAt = builder.activatedAt;
        this.closedAt = builder.closedAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public SchoolAcademicYearStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolAcademicYearStatus status) {
        this.status = status;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
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

    public static Builder builder() {
        return new SchoolAcademicYear.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private SchoolAcademicYearStatus status;
        private Instant activatedAt;
        private Instant closedAt;
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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder status(SchoolAcademicYearStatus status) {
            this.status = status;
            return this;
        }

        public Builder activatedAt(Instant activatedAt) {
            this.activatedAt = activatedAt;
            return this;
        }

        public Builder closedAt(Instant closedAt) {
            this.closedAt = closedAt;
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

        public SchoolAcademicYear build() {
            return new SchoolAcademicYear(this);
        }
    }
}
