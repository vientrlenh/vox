package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSessionJpaEntity;


public interface SpringDataAssessmentSessionRepository extends JpaRepository<AssessmentSessionJpaEntity, UUID> {

    // The four statements below EXCLUDE soft-deleted sessions: they serve business flows (entering the assessment, counting attempts,
    // finalizing scores, closing the assessment) rather than the admin lookup screen, so a deleted session must not
    // count as "the candidate already has a result". The other statements in the file already filter by a list of positive
    // statuses (IN_PROGRESS, SUBMITTED...) and so naturally never touch DELETED.
    @Query("""
        SELECT s FROM AssessmentSessionJpaEntity s
        WHERE s.assessmentId = :assessmentId AND s.candidateId = :candidateId AND s.status <> 'DELETED'
        ORDER BY s.startedAt DESC
        LIMIT 1
    """)
    Optional<AssessmentSessionJpaEntity> findTopByAssessmentIdAndCandidateIdOrderByStartedAtDesc(
        @Param("assessmentId") UUID assessmentId, @Param("candidateId") UUID candidateId);

    @Query("""
        SELECT s FROM AssessmentSessionJpaEntity s
        WHERE s.candidateId = :candidateId AND s.status <> 'DELETED'
        ORDER BY s.startedAt DESC
        LIMIT 1
    """)
    Optional<AssessmentSessionJpaEntity> findTopByCandidateIdOrderByStartedAtDesc(@Param("candidateId") UUID candidateId);

    Optional<AssessmentSessionJpaEntity> findTopByCandidateIdAndStatusInOrderByStartedAtDesc(UUID candidateId, Collection<String> statuses);

    @Query("SELECT s FROM AssessmentSessionJpaEntity s WHERE s.candidateId = :candidateId AND s.status <> 'DELETED'")
    List<AssessmentSessionJpaEntity> findByCandidateId(@Param("candidateId") UUID candidateId);

    @Query("SELECT s FROM AssessmentSessionJpaEntity s WHERE s.candidateId IN :candidateIds AND s.status <> 'DELETED'")
    List<AssessmentSessionJpaEntity> findByCandidateIdIn(@Param("candidateIds") Collection<UUID> candidateIds);

    /**
     * INTENTIONALLY counts soft-deleted sessions too: a deleted row still keeps {@code paper_id}, so if the paper could be deleted
     * that session would point to a paper that no longer exists and the admin would have no way to reopen it for cross-checking --
     * exactly what soft delete exists to preserve.
     */
    boolean existsByPaperId(UUID paperId);

    /**
     * Counts sessions still in progress for an assessment. Skips suspended candidates the same way
     * as {@code findPastScheduleEndCandidates} below -- their sessions do not end on their own.
     */
    @Query("""
        SELECT COUNT(s)
        FROM AssessmentSessionJpaEntity s
        JOIN AssessmentCandidateJpaEntity c ON c.id = s.candidateId
        WHERE s.assessmentId = :assessmentId
          AND s.status IN ('IN_PROGRESS', 'INTERRUPTED')
          AND c.blockedAt IS NULL
    """)
    long countActiveByAssessmentId(@Param("assessmentId") UUID assessmentId);

    @Query("""
        SELECT s
        FROM AssessmentSessionJpaEntity s
        JOIN AssessmentJpaEntity e ON e.id = s.assessmentId
        JOIN AssessmentCandidateJpaEntity c ON c.id = s.candidateId
        WHERE (e.status IN ('CLOSED', 'CANCELLED') OR (e.closeAt IS NOT NULL AND e.closeAt < :now))
          AND (
                s.status = 'EXPIRED'
                OR s.status = 'SUBMITTED'
                OR s.status IN ('IN_PROGRESS', 'INTERRUPTED')
              )
          AND (
                (e.kind = 'CENTRALIZED' AND c.status = 'ATTENDED')
                OR (e.kind = 'CLASS_TEST' AND c.status NOT IN ('ABSENT', 'EXEMPTED', 'CANCELLED'))
              )
          AND s.status NOT IN ('GRADING', 'GRADED', 'GRADING_FAILED')
        ORDER BY s.startedAt ASC
    """)
    List<AssessmentSessionJpaEntity> findDeferredGradingCandidates(@Param("now") Instant now);

    @Query("""
        SELECT s
        FROM AssessmentSessionJpaEntity s
        JOIN AssessmentCandidateJpaEntity c ON c.id = s.candidateId
        JOIN AssessmentScheduleJpaEntity sch ON sch.id = c.scheduleId
        WHERE sch.endDate < :threshold
          AND s.status IN ('IN_PROGRESS', 'INTERRUPTED')
          AND s.status NOT IN ('GRADING', 'GRADED', 'GRADING_FAILED')
          AND c.status = 'ATTENDED'
        ORDER BY sch.endDate ASC, s.startedAt ASC
    """)
    List<AssessmentSessionJpaEntity> findPastScheduleEndCandidates(@Param("threshold") java.time.Instant threshold);
    Optional<AssessmentSessionJpaEntity> findByAssessmentIdAndCandidateIdAndStatus(UUID assessmentId, UUID candidateId, String status);
    Optional<AssessmentSessionJpaEntity> findByIdAndStatusIn(UUID id, Collection<String> statuses);

    @Query("""
        SELECT s 
        FROM AssessmentSessionJpaEntity s 
        JOIN AssessmentJpaEntity e 
            ON s.assessmentId = e.id 
        WHERE s.id IN :ids
            AND s.startedAt <= :now 
            AND s.submittedAt IS NULL 
            AND s.status = 'IN_PROGRESS' 
            AND e.schoolId = :schoolId
    """)
    List<AssessmentSessionJpaEntity> findActiveByIdInAndSchoolId(@Param("ids") Collection<UUID> ids, @Param("now") Instant now, @Param("schoolId") UUID schoolId);

    @Modifying
    @Query("UPDATE AssessmentSessionJpaEntity s SET s.status = :to WHERE s.id = :id AND s.status = :from")
    int tryTransitionStatus(@Param("id") UUID id, @Param("from") String from, @Param("to") String to);

    /**
     * Fixes the stream type of an assessment session, only if it was never fixed (compare-and-set).
     *
     * <p>The condition {@code chosenStreamType IS NULL} is the lock itself: two token requests
     * running in parallel (bootstrap on entering the assessment and the first credential renewal) will have only one
     * able to write, the other gets 0 rows and must re-read the value that won. Using
     * {@code save(entity)} instead of this statement would lose atomicity and also overwrite the other
     * columns of the session (status, flagged...) with an old snapshot read earlier.
     *
     * @return the number of rows updated: 1 if fixed successfully, 0 if the session was already fixed.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AssessmentSessionJpaEntity s
        SET s.chosenStreamType = :chosenStreamType
        WHERE s.id = :id AND s.chosenStreamType IS NULL
    """)
    int lockChosenStreamType(@Param("id") UUID id, @Param("chosenStreamType") String chosenStreamType);

    /**
     * Writes the countdown clock checkpoint, only if the new value is smaller than the current one.
     *
     * <p>The condition {@code remainingSeconds > :remainingSeconds} is not an optimization but a security
     * constraint: this value is sent by the student's machine, so if free overwriting were allowed the checkpoint
     * endpoint would become a self-extension API for the assessment time. The clock may only go backwards.
     *
     * <p>It also handles races: the client checkpoints every 10 seconds and requests may
     * arrive out of order; an old packet arriving late is rejected by this very condition.
     *
     * @return 1 if written, 0 if the submitted value is not smaller than the stored one.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AssessmentSessionJpaEntity s
        SET s.remainingSeconds = :remainingSeconds
        WHERE s.id = :id
          AND (s.remainingSeconds IS NULL OR s.remainingSeconds > :remainingSeconds)
    """)
    int checkpointRemainingSeconds(@Param("id") UUID id, @Param("remainingSeconds") int remainingSeconds);

    /**
     * Soft-deletes an assessment session: marks it {@code DELETED} with a timestamp and a reason.
     *
     * <p>The condition {@code deletedAt IS NULL} makes this idempotent -- clicking delete twice (or two
     * people clicking) makes the later call get 0 rows instead of overwriting the reason and time of the first,
     * which must be preserved when an explanation is needed.
     *
     * @return 1 if just deleted, 0 if the session was already deleted.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AssessmentSessionJpaEntity s
        SET s.status = 'DELETED', s.deletedAt = :deletedAt, s.deletedReason = :reason
        WHERE s.id = :id AND s.deletedAt IS NULL
    """)
    int softDelete(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt, @Param("reason") String reason);
}
