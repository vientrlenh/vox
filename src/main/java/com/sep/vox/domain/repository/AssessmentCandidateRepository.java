package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;

public interface AssessmentCandidateRepository {
    AssessmentCandidate save(AssessmentCandidate candidate);
    List<AssessmentCandidate> saveAll(Collection<AssessmentCandidate> candidates);
    List<AssessmentCandidate> findByAssessmentId(UUID assessmentId);
    List<AssessmentCandidate> findByAssessmentIdIn(Collection<UUID> assessmentIds);
    /**
     * At most one candidate per assessment: the earliest assigned one (ties broken by id), the same one
     * {@link #findByAssessmentId} lists first. Assessments without candidates are absent from the result.
     */
    List<AssessmentCandidate> findFirstCandidateByAssessmentIdIn(Collection<UUID> assessmentIds);
    long countByAssessmentId(UUID assessmentId);
    Map<UUID, Long> countByAssessmentIdIn(Collection<UUID> assessmentIds);

    void deleteById(UUID id);

    /** Deletes a whole group in one statement — used for bulk delete on the candidate list screen. */
    void deleteByIdIn(Collection<UUID> ids);
    void deleteByAssessmentId(UUID assessmentId);

    Optional<AssessmentCandidate> findById(UUID id);
    Optional<AssessmentCandidate> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
    boolean existsByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
    List<AssessmentCandidate> findByStudentId(UUID studentId);
    List<AssessmentCandidate> findByScheduleId(UUID scheduleId);
    /** Candidates currently assigned exactly this paper — used to unassign before deleting the paper. */
    List<AssessmentCandidate> findByAssignedPaperId(UUID paperId);
    Set<UUID> findStudentIdsByAssessmentId(UUID assessmentId);
    List<UUID> findUnblockedStudentIdsByAssessmentId(UUID assessmentId);
    List<AssessmentCandidate> findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID assessmentId);
    List<AssessmentCandidate> findByIdIn(Collection<UUID> ids);
    List<AssessmentCandidate> findByIdInAndAssessmentId(Collection<UUID> ids, UUID assessmentId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByAssessmentIdAndScheduleIdIsNotNull(UUID assessmentId);
    Optional<AssessmentCandidate> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    List<AssessmentCandidate> findActiveCandidates(UUID studentId, Instant now);

    /**
     * For each student in {@code studentIds}, the schedule (DRAFT/PUBLISHED) they are already placed in that overlaps
     * in time with [start, end). Scans EVERY assessment, not only the open one: a student cannot sit in two rooms at the
     * same time. {@code excludeScheduleId} (if not null) is excluded from the check. A student who is free is not
     * present in the result.
     */
    List<StudentScheduleConflict> findConflictsForStudents(
            Collection<UUID> studentIds, Instant start, Instant end, UUID excludeScheduleId);

    /** One conflict: student {@code studentId} is already placed in schedule {@code scheduleId} running [start, end). */
    record StudentScheduleConflict(UUID studentId, UUID scheduleId, Instant startDate, Instant endDate) {
    }
}

