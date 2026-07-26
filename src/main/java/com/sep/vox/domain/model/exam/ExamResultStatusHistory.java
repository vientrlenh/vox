package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng nhật ký đổi trạng thái của bài thi: ai, lúc nào, từ đâu sang đâu, vì sao,
 * điểm trước và sau.
 *
 * <p>Chỉ ghi, không sửa. Mọi use case đổi
 * {@link ExamCandidateResultStatus} đều phải đi qua
 * {@code ResultStatusHistoryRecorder} — nếu có hai chỗ ghi thì tranh chấp điểm sẽ
 * không truy được nữa.
 */
public class ExamResultStatusHistory {
    private UUID id;
    private UUID candidateResultId;
    /** null khi đây là dòng đầu tiên (bài vừa được tạo). */
    private ExamCandidateResultStatus fromStatus;
    private ExamCandidateResultStatus toStatus;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private ResultStatusChangeSource source;
    /** null khi hệ thống tự đổi (AI chấm xong, job chốt sổ). */
    private UUID actorId;
    private String reason;
    private OffsetDateTime createdAt;

    public ExamResultStatusHistory() {}

    public ExamResultStatusHistory(UUID id, UUID candidateResultId, ExamCandidateResultStatus fromStatus,
            ExamCandidateResultStatus toStatus, BigDecimal scoreBefore, BigDecimal scoreAfter,
            ResultStatusChangeSource source, UUID actorId, String reason, OffsetDateTime createdAt) {
        this.id = id;
        this.candidateResultId = candidateResultId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.source = source;
        this.actorId = actorId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public ExamResultStatusHistory(UUID candidateResultId, ExamCandidateResultStatus fromStatus,
            ExamCandidateResultStatus toStatus, BigDecimal scoreBefore, BigDecimal scoreAfter,
            ResultStatusChangeSource source, UUID actorId, String reason, OffsetDateTime createdAt) {
        this.candidateResultId = candidateResultId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.source = source;
        this.actorId = actorId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    /** Trạng thái không đổi mà điểm đổi cũng là một sự kiện đáng ghi (hậu kiểm sửa điểm). */
    public boolean isScoreOnlyChange() {
        return fromStatus == toStatus;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCandidateResultId() {
        return candidateResultId;
    }

    public void setCandidateResultId(UUID candidateResultId) {
        this.candidateResultId = candidateResultId;
    }

    public ExamCandidateResultStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(ExamCandidateResultStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public ExamCandidateResultStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(ExamCandidateResultStatus toStatus) {
        this.toStatus = toStatus;
    }

    public BigDecimal getScoreBefore() {
        return scoreBefore;
    }

    public void setScoreBefore(BigDecimal scoreBefore) {
        this.scoreBefore = scoreBefore;
    }

    public BigDecimal getScoreAfter() {
        return scoreAfter;
    }

    public void setScoreAfter(BigDecimal scoreAfter) {
        this.scoreAfter = scoreAfter;
    }

    public ResultStatusChangeSource getSource() {
        return source;
    }

    public void setSource(ResultStatusChangeSource source) {
        this.source = source;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
