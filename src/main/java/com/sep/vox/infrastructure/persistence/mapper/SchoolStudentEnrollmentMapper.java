package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolStudentEnrollment;
import com.sep.vox.domain.model.school.SchoolStudentEnrollmentStatus;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolStudentEnrollmentJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SchoolStudentEnrollmentMapper {

    private SchoolStudentEnrollmentMapper() {}

    public static SchoolStudentEnrollment toDomain(SchoolStudentEnrollmentJpaEntity jpa) {
        try {
            return SchoolStudentEnrollment.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .academicYearId(jpa.getAcademicYearId())
                .schoolGradeId(jpa.getSchoolGradeId())
                .userId(jpa.getUserId())
                .status(SchoolStudentEnrollmentStatus.from(jpa.getStatus()))
                .enrolledOn(jpa.getEnrolledOn())
                .leftOn(jpa.getLeftOn())
                .leaveReason(jpa.getLeaveReason())
                .decidedAt(jpa.getDecidedAt())
                .decidedBy(jpa.getDecidedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school student enrollment entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static SchoolStudentEnrollmentJpaEntity toJpa(SchoolStudentEnrollment enrollment) {
        try {
            return SchoolStudentEnrollmentJpaEntity.builder()
                .id(enrollment.getId())
                .schoolId(enrollment.getSchoolId())
                .academicYearId(enrollment.getAcademicYearId())
                .schoolGradeId(enrollment.getSchoolGradeId())
                .userId(enrollment.getUserId())
                .status(SchoolStudentEnrollmentStatus.value(enrollment.getStatus()))
                .enrolledOn(enrollment.getEnrolledOn())
                .leftOn(enrollment.getLeftOn())
                .leaveReason(enrollment.getLeaveReason())
                .decidedAt(enrollment.getDecidedAt())
                .decidedBy(enrollment.getDecidedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school student enrollment model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
