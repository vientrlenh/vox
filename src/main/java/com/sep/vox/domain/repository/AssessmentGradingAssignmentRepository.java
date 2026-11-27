package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;

public interface AssessmentGradingAssignmentRepository {
    Optional<AssessmentGradingAssignment> findById(UUID id);

    /**
     * Same as {@link #findById} but locks the row for writing, and the lock is held until the end of the transaction. Only
     * call INSIDE a write transaction — for read-modify-write flows on the same assignment (grading, changing teacher,
     * setting a deadline) so that they queue up instead of overwriting each other.
     */
    Optional<AssessmentGradingAssignment> findByIdForUpdate(UUID id);

    /**
     * The open assignment of a result. A result has at most one open row — the DB enforces it with a
     * unique index on {@code active_result_id}; closed rows of earlier rounds
     * carry {@code active_result_id = NULL} so they do not appear here.
     */
    Optional<AssessmentGradingAssignment> findOpenByCandidateResultId(UUID candidateResultId);

    List<AssessmentGradingAssignment> findOpenByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** The whole assignment history of a result, newest first. */
    List<AssessmentGradingAssignment> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId);

    List<AssessmentGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /**
     * Has this teacher ever been assigned to grade that attempt -- any round, any status.
     *
     * <p>Deliberately counts {@code COMPLETED} rows too: for an appealed attempt, the grader of the previous round
     * is the one being asked again, and by then their assignment row is closed. Used for READ-ONLY checks
     * (see the assessment recording); every score WRITE action still has to go through
     * {@code AssessmentGradingAccessService.authorizeAssignedTeacher} on the row that is actually open.
     */
    boolean existsByCandidateResultIdAndTeacherId(UUID candidateResultId, UUID teacherId);

    /** The appeal round of an appeal (every status). */
    List<AssessmentGradingAssignment> findByAppealId(UUID appealId);

    /** Open assignments past their deadline — the source of the reminder job and of bulk revocation. */
    List<AssessmentGradingAssignment> findOverdue(Instant now);

    /**
     * Overdue within the scope of one school (and one assessment if {@code assessmentId != null}), oldest
     * first, at most {@code limit} rows. Bulk reclaiming uses this so it does not
     * touch another school's data and so one pass does not pull the whole school into a single write transaction.
     */
    List<AssessmentGradingAssignment> findOverdueInSchool(
        Instant now, UUID schoolId, UUID assessmentId, int limit);

    /** Past the deadline and never sent a reminder mail. Duplicate reminders are prevented with {@code reminded_at}. */
    List<AssessmentGradingAssignment> findDueForReminder(Instant threshold);

    AssessmentGradingAssignment save(AssessmentGradingAssignment assignment);

    List<AssessmentGradingAssignment> saveAll(List<AssessmentGradingAssignment> assignments);

    void deleteById(UUID id);

    /** Cleans up per result when an assessment session is deleted — no FK blocks it, and missing one leaves an orphan row. */
    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    long countByTeacherIdAndStatus(UUID teacherId, GradingAssignmentStatus status);
}
