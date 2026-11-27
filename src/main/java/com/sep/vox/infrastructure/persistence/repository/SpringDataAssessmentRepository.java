package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity;

public interface SpringDataAssessmentRepository extends JpaRepository<AssessmentJpaEntity, UUID> {

    @Query("""
        SELECT e
        FROM AssessmentJpaEntity e
        WHERE (:schoolId IS NULL OR e.schoolId = :schoolId)
          AND (
                :schoolClassId IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM AssessmentCandidateJpaEntity ec
                    JOIN SchoolClassUserJpaEntity scu ON scu.userId = ec.studentId
                    WHERE ec.assessmentId = e.id
                      AND scu.schoolClassId = :schoolClassId
                      AND scu.isActive = true
                )
              )
          AND (:kind IS NULL OR e.kind = :kind)
          AND (:status IS NULL OR e.status = :status)
          AND (
                :keywordPattern IS NULL
                OR LOWER(e.code) LIKE :keywordPattern
                OR LOWER(e.name) LIKE :keywordPattern
              )
          AND (
                :systemAdmin = true
                OR (:schoolAdmin = true AND e.schoolId = :currentSchoolId)
                OR EXISTS (
                    SELECT 1
                    FROM AssessmentMemberJpaEntity em
                    WHERE em.assessmentId = e.id
                      AND em.userId = :currentUserId
                )
                OR (e.status IN ('CLOSED', 'RESULTS_PUBLISHED') AND e.schoolId = :currentSchoolId)
              )
        ORDER BY e.updatedAt DESC
    """)
    Page<AssessmentJpaEntity> findAccessible(
        @Param("currentUserId") UUID currentUserId,
        @Param("currentSchoolId") UUID currentSchoolId,
        @Param("systemAdmin") boolean systemAdmin,
        @Param("schoolAdmin") boolean schoolAdmin,
        @Param("schoolId") UUID schoolId,
        @Param("schoolClassId") UUID schoolClassId,
        @Param("kind") String kind,
        @Param("status") String status,
        @Param("keywordPattern") String keywordPattern,
        Pageable pageable
    );

    List<com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentJpaEntity> findAllByBlueprintId(UUID blueprintId);
    boolean existsByBlueprintId(UUID blueprintId);

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM AssessmentJpaEntity e
        WHERE e.blueprintId = :blueprintId
          AND e.kind = :kind
          AND e.status <> :status
    """)
    boolean existsByBlueprintIdAndKindAndStatusNot(
        @Param("blueprintId") UUID blueprintId,
        @Param("kind") String kind,
        @Param("status") String status
    );

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM AssessmentSessionJpaEntity s
        WHERE s.assessmentId = :assessmentId
          AND s.submittedAt IS NOT NULL
    """)
    boolean existsSubmittedSessionByAssessmentId(@Param("assessmentId") UUID assessmentId);

    /**
     * Assessments that can still be graded, have results waiting for AI score review, and have NOT been reminded even once.
     *
     * <p>Both IN_PROGRESS and CLOSED are included on purpose: a schedule that ended early already produced results while
     * the assessment was still running, so waiting until it closes would throw away most of the gradable time.
     * CLOSED stays in the list so an assessment closed before the sweep got to run is not missed.
     *
     * <p>Native + {@code FOR UPDATE SKIP LOCKED} for the same reason as
     * {@code findDueForReminder}: {@code human_grading_notified_at} alone only prevents duplicates
     * across RUNS, not across INSTANCES -- two replicas both reading before either
     * commits both see NULL and both emit the event.
     */
    @Query(value = """
        SELECT * FROM assessments e
        WHERE e.status IN ('IN_PROGRESS', 'CLOSED')
        AND e.human_grading_notified_at IS NULL
        AND EXISTS (
            SELECT 1 FROM assessment_candidate_results r
            WHERE r.assessment_id = e.id AND r.status = 'PENDING_REVIEW'
        )
        ORDER BY e.created_at ASC
        LIMIT 200
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<AssessmentJpaEntity> findDueForHumanGradingNotice();

    List<AssessmentJpaEntity> findByStatusAndOpenAtBefore(String status, Instant time);
    List<AssessmentJpaEntity> findByStatusAndCloseAtBefore(String status, Instant time);
}
