package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SchoolStudentEnrollment {
    private UUID id;
    private UUID schoolId;
    private UUID academicYearId;
    private UUID schoolGradeId;
    private UUID userId;
    private SchoolStudentEnrollmentStatus status;
    private LocalDate enrolledOn;
    private LocalDate leftOn;
    private String leaveReason;
    private Instant decidedAt;
    private UUID decidedBy;

    protected SchoolStudentEnrollment() {}

    public SchoolStudentEnrollment(UUID id, UUID schoolId, UUID academicYearId, UUID schoolGradeId, UUID userId,
            SchoolStudentEnrollmentStatus status, LocalDate enrolledOn, LocalDate leftOn, String leaveReason,
            Instant decidedAt, UUID decidedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.academicYearId = academicYearId;
        this.schoolGradeId = schoolGradeId;
        this.userId = userId;
        this.status = status;
        this.enrolledOn = enrolledOn;
        this.leftOn = leftOn;
        this.leaveReason = leaveReason;
        this.decidedAt = decidedAt;
        this.decidedBy = decidedBy;
    }

    public SchoolStudentEnrollment(Builder builder) {
        this.id = builder.id;
        this.schoolId = builder.schoolId;
        this.academicYearId = builder.academicYearId;
        this.schoolGradeId = builder.schoolGradeId;
        this.userId = builder.userId;
        this.status = builder.status;
        this.enrolledOn = builder.enrolledOn;
        this.leftOn = builder.leftOn;
        this.leaveReason = builder.leaveReason;
        this.decidedAt = builder.decidedAt;
        this.decidedBy = builder.decidedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(UUID academicYearId) {
        this.academicYearId = academicYearId;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public SchoolStudentEnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolStudentEnrollmentStatus status) {
        this.status = status;
    }

    public LocalDate getEnrolledOn() {
        return enrolledOn;
    }

    public void setEnrolledOn(LocalDate enrolledOn) {
        this.enrolledOn = enrolledOn;
    }

    public LocalDate getLeftOn() {
        return leftOn;
    }

    public void setLeftOn(LocalDate leftOn) {
        this.leftOn = leftOn;
    }

    public String getLeaveReason() {
        return leaveReason;
    }

    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public static Builder builder() {
        return new SchoolStudentEnrollment.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private UUID academicYearId;
        private UUID schoolGradeId;
        private UUID userId;
        private SchoolStudentEnrollmentStatus status;
        private LocalDate enrolledOn;
        private LocalDate leftOn;
        private String leaveReason;
        private Instant decidedAt;
        private UUID decidedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }

        public Builder academicYearId(UUID academicYearId) {
            this.academicYearId = academicYearId;
            return this;
        }

        public Builder schoolGradeId(UUID schoolGradeId) {
            this.schoolGradeId = schoolGradeId;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder status(SchoolStudentEnrollmentStatus status) {
            this.status = status;
            return this;
        }

        public Builder enrolledOn(LocalDate enrolledOn) {
            this.enrolledOn = enrolledOn;
            return this;
        }

        public Builder leftOn(LocalDate leftOn) {
            this.leftOn = leftOn;
            return this;
        }

        public Builder leaveReason(String leaveReason) {
            this.leaveReason = leaveReason;
            return this;
        }

        public Builder decidedAt(Instant decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public Builder decidedBy(UUID decidedBy) {
            this.decidedBy = decidedBy;
            return this;
        }

        public SchoolStudentEnrollment build() {
            return new SchoolStudentEnrollment(this);
        }
    }
}
