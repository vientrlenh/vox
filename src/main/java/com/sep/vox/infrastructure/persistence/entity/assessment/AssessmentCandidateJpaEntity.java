package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_candidates", indexes = {
    @Index(name = "uq_assessment_candidates_assessment_student", columnList = "assessment_id, student_id", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentCandidateJpaEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "assigned_paper_id")
    private UUID assignedPaperId;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_candidates_status_valid", 
            constraint = "status IN ('ASSIGNED', 'ATTENDED', 'ABSENT', 'COMPLETED', 'EXEMPTED', 'CANCELLED')"
        )
    })
    private String status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "assigned_by", updatable = false)
    private UUID assignedBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
