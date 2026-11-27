package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;

public interface AssessmentSessionRepository {
    Optional<AssessmentSession> findById(UUID id);
    Optional<AssessmentSession> findLatestByAssessmentIdAndCandidateId(UUID assessmentId, UUID candidateId);
    Optional<AssessmentSession> findLatestByCandidateId(UUID candidateId);
    Optional<AssessmentSession> findLatestByCandidateIdAndStatuses(UUID candidateId, Collection<AssessmentSessionStatus> statuses);
    List<AssessmentSession> findByCandidateId(UUID candidateId);
    List<AssessmentSession> findByCandidateIdIn(Collection<UUID> candidateIds);
    /**
     * The number of sessions still in progress ({@link AssessmentSessionStatus#RESUMABLE}) of a class test.
     *
     * <p>Skips candidates who have been suspended ({@code blockedAt != null}) -- their session will never
     * end by itself, and keeping it would let one suspension also lock the close button. The same reason that
     * {@code AssessmentScheduleTimeoutGradingJob} skips them.
     */
    long countActiveByAssessmentId(UUID assessmentId);

    List<AssessmentSession> findDeferredGradingCandidates(Instant now);
    List<AssessmentSession> findPastScheduleEndCandidates(Instant threshold);
    boolean existsById(UUID id);
    /**
     * Whether any assessment session has been built on this paper variant. {@code assessment_sessions.paper_id} is a NOT NULL column
     * so it cannot be unlinked like {@code assessment_candidates.assigned_paper_id} -- only used to block deleting the paper variant.
     */
    boolean existsByPaperId(UUID paperId);
    AssessmentSession save(AssessmentSession session);
    void deleteById(UUID id);

    /**
     * Soft delete: marks {@code DELETED} with a timestamp and reason, keeping all of the answer data.
     *
     * @return 1 if just deleted, 0 if the session was already deleted earlier (idempotent).
     */
    int softDelete(UUID id, Instant deletedAt, String reason);
    /**
     * Assessment sessions in one of the {@link AssessmentSessionStatus#RESUMABLE} states.
     */
    Optional<AssessmentSession> findByIdAndResumable(UUID id);
    Optional<AssessmentSession> findActiveByAssessmentIdAndCandidateId(UUID assessmentId, UUID candidateId);
    List<AssessmentSession> findActiveByIdInAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId);

    /** Atomic conditional transition: only succeeds if current status equals {@code from}. Returns false if another writer already moved it. */
    boolean tryTransitionStatus(UUID sessionId, AssessmentSessionStatus from, AssessmentSessionStatus to);

    /**
     * Fixes the stream type of the assessment session if not yet fixed.
     *
     * @return 1 if written, 0 if the assessment session had already fixed its stream type.
     */
    int lockChosenStreamType(UUID id, AssessmentRequiredStreamType chosenStreamType);

    /**
     * Writes the countdown clock checkpoint, only when the new value is smaller than the current one.
     *
     * @return 1 if written, 0 if rejected because it is not smaller.
     */
    int checkpointRemainingSeconds(UUID id, int remainingSeconds);
}
