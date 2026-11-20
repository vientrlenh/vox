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
     * Xoá mềm: đánh dấu {@code DELETED} kèm thời điểm và lý do, giữ nguyên toàn bộ dữ liệu bài làm.
     *
     * @return 1 nếu vừa xoá, 0 nếu phiên đã bị xoá từ trước (idempotent).
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
     * Chốt loại stream cho phiên thi nếu chưa chốt.
     *
     * @return 1 nếu ghi được, 0 nếu phiên thi đã chốt loại stream từ trước.
     */
    int lockChosenStreamType(UUID id, AssessmentRequiredStreamType chosenStreamType);

    /**
     * Ghi checkpoint đồng hồ đếm ngược, chỉ khi giá trị mới nhỏ hơn giá trị đang có.
     *
     * @return 1 nếu ghi được, 0 nếu bị từ chối vì không nhỏ hơn.
     */
    int checkpointRemainingSeconds(UUID id, int remainingSeconds);
}
