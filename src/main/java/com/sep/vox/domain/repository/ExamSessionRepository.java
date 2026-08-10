package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;

public interface ExamSessionRepository {
    Optional<ExamSession> findById(UUID id);
    Optional<ExamSession> findLatestByExamIdAndCandidateId(UUID examId, UUID candidateId);
    Optional<ExamSession> findLatestByCandidateId(UUID candidateId);
    Optional<ExamSession> findLatestByCandidateIdAndStatuses(UUID candidateId, Collection<ExamSessionStatus> statuses);
    List<ExamSession> findAllByCandidateId(UUID candidateId);
    List<ExamSession> findAllByCandidateIdIn(Collection<UUID> candidateIds);
    /**
     * Số phiên còn đang làm bài ({@link ExamSessionStatus#RESUMABLE}) của một bài kiểm tra.
     *
     * <p>Bỏ qua thí sinh đã bị đình chỉ ({@code blockedAt != null}) — phiên của họ sẽ không bao giờ
     * tự kết thúc, giữ lại thì một lệnh đình chỉ sẽ khoá luôn nút đóng bài. Cùng lý do mà
     * {@code ExamScheduleTimeoutGradingJob} bỏ qua họ.
     */
    long countActiveByExamId(UUID examId);

    List<ExamSession> findDeferredGradingCandidates(java.time.Instant now);
    List<ExamSession> findPastScheduleEndCandidates(java.time.Instant threshold);
    boolean existsById(UUID id);
    /**
     * Đã có phiên thi nào dựng trên mã đề này chưa. {@code exam_sessions.paper_id} là cột NOT NULL
     * nên không gỡ được như {@code exam_candidates.assigned_paper_id} — chỉ dùng để chặn xoá mã đề.
     */
    boolean existsByPaperId(UUID paperId);
    ExamSession save(ExamSession session);
    void deleteById(UUID id);
    /**
     * Phiên thi đang ở một trong các trạng thái {@link ExamSessionStatus#RESUMABLE}.
     */
    Optional<ExamSession> findByIdAndResumable(UUID id);
    Optional<ExamSession> findActiveByExamIdAndCandidateId(UUID examId, UUID candidateId);
    List<ExamSession> findActiveByIdInAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId);

    /** Atomic conditional transition: only succeeds if current status equals {@code from}. Returns false if another writer already moved it. */
    boolean tryTransitionStatus(UUID sessionId, ExamSessionStatus from, ExamSessionStatus to);

    /**
     * Chốt loại stream cho phiên thi nếu chưa chốt.
     *
     * @return 1 nếu ghi được, 0 nếu phiên thi đã chốt loại stream từ trước.
     */
    int lockChosenStreamType(UUID id, ExamRequiredStreamType chosenStreamType);

    /**
     * Ghi checkpoint đồng hồ đếm ngược, chỉ khi giá trị mới nhỏ hơn giá trị đang có.
     *
     * @return 1 nếu ghi được, 0 nếu bị từ chối vì không nhỏ hơn.
     */
    int checkpointRemainingSeconds(UUID id, int remainingSeconds);
}
