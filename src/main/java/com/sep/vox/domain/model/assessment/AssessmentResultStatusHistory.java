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
    /** Null when this is the first row (the result has just been created). */
    private AssessmentCandidateResultStatus fromStatus;
    private AssessmentCandidateResultStatus toStatus;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private ResultStatusChangeSource source;
    /** Null when the system changed the status itself (AI grading finished, the finalizing job). */
    private UUID actorId;
    private String reason;
    private Instant createdAt;

    public AssessmentResultStatusHistory() {}

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentResultStatusHistory target = new AssessmentResultStatusHistory();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder candidateResultId(UUID candidateResultId) {
            target.candidateResultId = candidateResultId;
            return this;
        }

        public Builder fromStatus(AssessmentCandidateResultStatus fromStatus) {
            target.fromStatus = fromStatus;
            return this;
        }

        public Builder toStatus(AssessmentCandidateResultStatus toStatus) {
            target.toStatus = toStatus;
            return this;
        }

        public Builder scoreBefore(BigDecimal scoreBefore) {
            target.scoreBefore = scoreBefore;
            return this;
        }

        public Builder scoreAfter(BigDecimal scoreAfter) {
            target.scoreAfter = scoreAfter;
            return this;
        }

        public Builder source(ResultStatusChangeSource source) {
            target.source = source;
            return this;
        }

        public Builder actorId(UUID actorId) {
            target.actorId = actorId;
            return this;
        }

        public Builder reason(String reason) {
            target.reason = reason;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public AssessmentResultStatusHistory build() {
            return target;
        }

    }
}
