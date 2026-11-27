package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentSession {
    private UUID id;
    private UUID assessmentId;
    private UUID candidateId;
    private UUID paperId;
    private Instant startedAt;
    private Instant submittedAt;
    private AssessmentSessionStatus status;
    private boolean flagged;
    private String flagReason;

    /**
     * The stream type that has been fixed for this assessment session, null if no stream token has been issued yet.
     *
     * <p>Only meaningful when the assessment sets {@code streamTypePermission = ANY} - then the learner may
     * choose among the types the assessment accepts, and that choice must be recorded instead of only
     * living in the JWT: the token is issued many times in one session (reconnect, renewing the upload
     * credentials), so without storing it every reissue is a chance to switch to a lighter proctoring type
     * in the middle of the assessment. Written once and then locked - see
     * {@code AssessmentSessionRepository#lockChosenStreamType}.
     *
     * <p>It is also the evidence to tell "the learner chose not to stream the screen" from
     * "the screen stream failed": both lead to a missing screen recording in storage.
     */
    private AssessmentRequiredStreamType chosenStreamType;

    /**
     * Checkpoint of the countdown clock sent by the client, null if the session has never been checkpointed.
     *
     * <p>The client has to report it because the server cannot derive it: the WPF clock <b>does not
     * subtract the seconds while the avatar is speaking</b>, so the remaining time is not
     * {@code startedAt + duration - now}. Only the client knows how long the avatar has spoken.
     *
     * <p>Without this value every re-entry into the session would restart the countdown from the beginning:
     * a learner who leaves at minute 25 of a 30-minute assessment would get another full 30 minutes.
     */
    private Integer remainingSeconds;

    /**
     * Why the most recent AI grading failed; null when the session is not in {@link AssessmentSessionStatus#GRADING_FAILED}.
     *
     * <p>null WHILE failing is also a meaningful answer, not missing data: the DLT branch
     * marks a session failed when the message has run out of retries, and there is no reason to store there. Being able to
     * tell "failed because of X" from "failed, reason unknown" is the whole value of the triage page.
     */
    private String gradingError;

    /** How many times the grading service tried before giving up; null exactly when {@link #gradingError} is null. */
    private Integer gradingRetryCount;

    /**
     * How many times the SCHOOL has asked the AI to grade this session again.
     *
     * <p>Quite different from {@link #gradingRetryCount}: that column is how many times the GRADING SERVICE
     * retried by itself within one round, taken from the Kafka event. This column counts the ROUNDS
     * requested by a person at the school.
     *
     * <p>It counts ONLY the school's rounds. Rounds triggered by the system administrator, and later by a job that
     * reruns automatically after a widespread incident, do not use up the school's allowance: otherwise one
     * mass AI failure would silently burn the regrade allowance of every school and force hundreds of results
     * into manual grading because of an incident on the platform itself.
     */
    private int schoolRegradeCount;

    public AssessmentSession() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public AssessmentSessionStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentSessionStatus status) {
        this.status = status;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public AssessmentRequiredStreamType getChosenStreamType() {
        return chosenStreamType;
    }

    public void setChosenStreamType(AssessmentRequiredStreamType chosenStreamType) {
        this.chosenStreamType = chosenStreamType;
    }

    public Integer getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Integer remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getGradingError() {
        return gradingError;
    }

    /**
     * Only set together with {@code setStatus(GRADING_FAILED)}: the constraint
     * {@code chk_assessment_sessions_grading_error_only_when_failed} in the DB rejects a reason on a session that did not fail.
     */
    public void setGradingError(String gradingError) {
        this.gradingError = gradingError;
    }

    public Integer getGradingRetryCount() {
        return gradingRetryCount;
    }

    public void setGradingRetryCount(Integer gradingRetryCount) {
        this.gradingRetryCount = gradingRetryCount;
    }

    /**
     * Clears the traces of the previous failed grading. Must be called on EVERY path that leaves {@code GRADING_FAILED}:
     * if the old message stayed, a session that was re-graded successfully would still sit in the incident group on the
     * triage page, and the person on duty would handle something that is already done.
     */
    public int getSchoolRegradeCount() {
        return schoolRegradeCount;
    }

    public void setSchoolRegradeCount(int schoolRegradeCount) {
        this.schoolRegradeCount = schoolRegradeCount;
    }

    /**
     * Whether the school may still ask the AI to re-grade this session.
     *
     * <p>One attempt: a transient error almost always disappears on the first retry, so a failure on the second attempt
     * is fairly strong evidence that the cause is deterministic (corrupt audio, wrong rubric configuration) —
     * trying more only burns money on a failure already known in advance. The way out at that point is to hand it to a grader.
     */
    public boolean canSchoolRequestAiRegrade() {
        return schoolRegradeCount < MAX_SCHOOL_REGRADES;
    }

    /** Maximum number of AI re-grades the school may request for one session. */
    public static final int MAX_SCHOOL_REGRADES = 1;

    public void clearGradingFailure() {
        this.gradingError = null;
        this.gradingRetryCount = null;
    }

    public static AssessmentSession started(UUID assessmentId, UUID candidateId, UUID paperId, Instant startedAt) {
        return builder()
            .assessmentId(assessmentId)
            .candidateId(candidateId)
            .paperId(paperId)
            .startedAt(startedAt)
            .submittedAt(null)
            .status(AssessmentSessionStatus.IN_PROGRESS)
            .flagged(false)
            .flagReason(null)
            .build();
    }

    public static AssessmentSession gradedWithoutAnswers(UUID assessmentId, UUID candidateId, UUID paperId, Instant at) {
        return builder()
            .assessmentId(assessmentId)
            .candidateId(candidateId)
            .paperId(paperId)
            .startedAt(at)
            .submittedAt(at)
            .status(AssessmentSessionStatus.GRADED)
            .flagged(false)
            .flagReason(null)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentSession target = new AssessmentSession();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder assessmentId(UUID assessmentId) {
            target.assessmentId = assessmentId;
            return this;
        }

        public Builder candidateId(UUID candidateId) {
            target.candidateId = candidateId;
            return this;
        }

        public Builder paperId(UUID paperId) {
            target.paperId = paperId;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            target.startedAt = startedAt;
            return this;
        }

        public Builder submittedAt(Instant submittedAt) {
            target.submittedAt = submittedAt;
            return this;
        }

        public Builder status(AssessmentSessionStatus status) {
            target.status = status;
            return this;
        }

        public Builder flagged(boolean flagged) {
            target.flagged = flagged;
            return this;
        }

        public Builder flagReason(String flagReason) {
            target.flagReason = flagReason;
            return this;
        }

        public Builder chosenStreamType(AssessmentRequiredStreamType chosenStreamType) {
            target.chosenStreamType = chosenStreamType;
            return this;
        }

        public Builder remainingSeconds(Integer remainingSeconds) {
            target.remainingSeconds = remainingSeconds;
            return this;
        }

        public Builder gradingError(String gradingError) {
            target.gradingError = gradingError;
            return this;
        }

        public Builder gradingRetryCount(Integer gradingRetryCount) {
            target.gradingRetryCount = gradingRetryCount;
            return this;
        }

        public Builder schoolRegradeCount(int schoolRegradeCount) {
            target.schoolRegradeCount = schoolRegradeCount;
            return this;
        }

        public AssessmentSession build() {
            return target;
        }

    }
}
