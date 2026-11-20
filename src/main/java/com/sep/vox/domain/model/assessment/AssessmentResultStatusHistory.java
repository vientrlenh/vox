package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the status-change log of an attempt: who, when, from what to what, why,
 * and the score before and after.
 *
 * <p>Write-only, never edited. Every use case that changes
 * {@link AssessmentCandidateResultStatus} must go through
 * {@code ResultStatusHistoryRecorder} -- if there were two write points a score dispute could
 * no longer be traced.
 */
public class AssessmentResultStatusHistory {
    private UUID id;
    private UUID candidateResultId;
    /** null khi đây là dòng đầu tiên (bài vừa được tạo). */
    private AssessmentCandidateResultStatus fromStatus;
    private AssessmentCandidateResultStatus toStatus;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private ResultStatusChangeSource source;
    /** null khi hệ thống tự đổi (AI chấm xong, job chốt sổ). */
    private UUID actorId;
    private String reason;
    private Instant createdAt;

    public AssessmentResultStatusHistory() {}

    public AssessmentResultStatusHistory(UUID id, UUID candidateResultId, AssessmentCandidateResultStatus fromStatus,
            AssessmentCandidateResultStatus toStatus, BigDecimal scoreBefore, BigDecimal scoreAfter,
            ResultStatusChangeSource source, UUID actorId, String reason, Instant createdAt) {
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

    public AssessmentResultStatusHistory(UUID candidateResultId, AssessmentCandidateResultStatus fromStatus,
            AssessmentCandidateResultStatus toStatus, BigDecimal scoreBefore, BigDecimal scoreAfter,
            ResultStatusChangeSource source, UUID actorId, String reason, Instant createdAt) {
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

    public AssessmentCandidateResultStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(AssessmentCandidateResultStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public AssessmentCandidateResultStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(AssessmentCandidateResultStatus toStatus) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
