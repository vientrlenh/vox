package com.sep.vox.domain.model.school;

import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.valueobject.PositiveInteger;

public class SchoolAcademicTerm {
    private UUID id;
    private UUID academicYearId;
    private String name;
    private PositiveInteger order;
    private LocalDate startDate;
    private LocalDate endDate;

    protected SchoolAcademicTerm() {}

    public SchoolAcademicTerm(UUID id, UUID academicYearId, String name, PositiveInteger order, LocalDate startDate,
            LocalDate endDate) {
        this.id = id;
        this.academicYearId = academicYearId;
        this.name = name;
        this.order = order;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public SchoolAcademicTerm(Builder builder) {
        this.id = builder.id;
        this.academicYearId = builder.academicYearId;
        this.name = builder.name;
        this.order = builder.order;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(UUID academicYearId) {
        this.academicYearId = academicYearId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PositiveInteger getOrder() {
        return order;
    }

    public void setOrder(PositiveInteger order) {
        this.order = order;
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

    public static Builder builder() {
        return new SchoolAcademicTerm.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID academicYearId;
        private String name;
        private PositiveInteger order;
        private LocalDate startDate;
        private LocalDate endDate;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder academicYearId(UUID academicYearId) {
            this.academicYearId = academicYearId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder order(PositiveInteger order) {
            this.order = order;
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

        public SchoolAcademicTerm build() {
            return new SchoolAcademicTerm(this);
        }
    }
}
