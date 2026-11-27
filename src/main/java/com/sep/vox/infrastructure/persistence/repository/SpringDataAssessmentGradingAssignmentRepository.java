package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataAssessmentGradingAssignmentRepository
        extends JpaRepository<AssessmentGradingAssignmentJpaEntity, UUID> {

    /**
     * Read with a write lock for the flows that MODIFY an assignment.
     *
     * <p>The domain is mapped to a detached POJO, so {@code save} is a {@code merge} that overwrites ALL
     * columns -- there is no dirty checking to detect that someone just changed this row. The lock here
     * is what forces two flows editing the same assignment to queue up, so checks such as
     * {@code isCompleted()} read the latest state instead of a stale snapshot.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ga FROM AssessmentGradingAssignmentJpaEntity ga WHERE ga.id = :id")
    Optional<AssessmentGradingAssignmentJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * The open row of a result. Filters by {@code active_result_id} rather than by
     * {@code candidate_result_id + status}: the active column is exactly what the unique index
     * guards, so the two places always say the same thing.
     */
    Optional<AssessmentGradingAssignmentJpaEntity> findByActiveResultId(UUID activeResultId);

    List<AssessmentGradingAssignmentJpaEntity> findByActiveResultIdIn(Collection<UUID> activeResultIds);

    List<AssessmentGradingAssignmentJpaEntity> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId);

    List<AssessmentGradingAssignmentJpaEntity> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** Goes straight into {@code idx_grading_assignments_result} and then filters the teacher on the few rows of the result. */
    boolean existsByCandidateResultIdAndTeacherId(UUID candidateResultId, UUID teacherId);

    List<AssessmentGradingAssignmentJpaEntity> findByAppealId(UUID appealId);

    @Query("""
        SELECT ga FROM AssessmentGradingAssignmentJpaEntity ga
        WHERE ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now
        ORDER BY ga.deadlineAt ASC
    """)
    List<AssessmentGradingAssignmentJpaEntity> findOverdue(@Param("now") Instant now);

    /**
     * Overdue WITHIN a school (and an assessment, if given). Bulk revocation goes this way
     * instead of {@link #findOverdue} + filtering in Java: the old version scanned the whole system and then called the
     * access service per row to read the schoolId -- 4N+1 queries, and the response time
     * of one school depended on the data of other schools.
     *
     * <p>{@code Pageable} here is the CAP of one revocation run, not pagination for the
     * UI: one click on "Revoke all" must not pull a whole school into one write transaction.
     * Oldest first so the next run continues exactly where the previous one stopped.
     */
    @Query("""
        SELECT ga FROM AssessmentGradingAssignmentJpaEntity ga
        JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
        JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
        WHERE ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now
        AND e.schoolId = :schoolId
        AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
        ORDER BY ga.deadlineAt ASC
    """)
    List<AssessmentGradingAssignmentJpaEntity> findOverdueInSchool(
        @Param("now") Instant now,
        @Param("schoolId") UUID schoolId,
        @Param("assessmentId") UUID assessmentId,
        Pageable pageable);

    /**
     * Approaching or past due and not yet reminded even once -- {@code reminded_at} is the duplicate guard.
     *
     * <p>Native + {@code FOR UPDATE SKIP LOCKED} because {@code reminded_at} alone only
     * prevents duplicates across RUNS, not across INSTANCES: two replicas
     * both reading before either commits both see {@code NULL} and both send
     * the mail. {@code SKIP LOCKED} lets them split different rows; {@code LIMIT}
     * stops one run from taking thousands of rows in one transaction.
     */
    @Query(value = """
        SELECT * FROM assessment_grading_assignments
        WHERE status = 'ASSIGNED' AND reminded_at IS NULL
        AND deadline_at IS NOT NULL AND deadline_at < :threshold
        ORDER BY deadline_at ASC
        LIMIT 200
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<AssessmentGradingAssignmentJpaEntity> findDueForReminder(@Param("threshold") Instant threshold);

    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    long countByTeacherIdAndStatus(UUID teacherId, String status);
}
