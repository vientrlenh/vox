package com.sep.vox.infrastructure.persistence.entity.school;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "school_student_enrollments", indexes = {
    @Index(columnList = "academic_year_id, user_id", name = "idx_school_student_enrollments_year_user", unique = true),
    @Index(columnList = "school_grade_id", name = "idx_school_student_enrollments_grade"),
    @Index(columnList = "school_id, user_id", name = "idx_school_student_enrollments_school_user")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class SchoolStudentEnrollmentJpaEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false, updatable = false)
    private UUID academicYearId;

    @Column(name = "school_grade_id", nullable = false)
    private UUID schoolGradeId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_student_enrollments_status_valid",
            constraint = "status IN ('ENROLLED', 'PROMOTED', 'RETAINED', 'GRADUATED', 'LEFT')"
        )
    })
    private String status;

    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;

    @Column(name = "left_on")
    private LocalDate leftOn;

    @Column(name = "leave_reason", length = 255)
    private String leaveReason;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

}
