package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * A batch of violations detected during assessment proctoring, published by vox-streaming on the topic
 * {@code assessment.alert.raised}.
 *
 * <p>This is an append-only ledger: no business edits, no deletes. The reason is its purpose --
 * evidence for decisions about the validity of an attempt. A piece of evidence that can be edited is
 * no longer evidence.
 *
 * <p>It does <b>not</b> take part in scoring. An alert is an AI detection with a confidence level, running
 * on the webcam, with false positives; while the criterion scores measure language ability. The two sit on two
 * different axes, and letting an alert drag a score down means punishing a suspicion of cheating by pretending
 * the learner spoke worse than they actually did -- both wrong to the learner and impossible to audit. The right place for it is the
 * uphold/regrade/invalidate decision, and the suspicion flag on the assessment session.
 */
public class AssessmentProctoringAlert {

    /**
     * The alert types that raise the question "was this attempt done by the candidate THEMSELF" -- and therefore
     * force the attempt to go through a reviewer. See {@code RecordAssessmentAttemptEvaluationUseCase}.
     *
     * <p><b>By TYPE, not by {@code level}.</b> This gate used to ask {@code level = 'CRITICAL'},
     * which uses one scale to answer two different questions. The level scale of vox-streaming
     * ({@code DefaultAlertLevel}) is defined by WHAT THE PROCTOR MUST DO RIGHT NOW DURING THE SESSION --
     * "CRITICAL: the proctor stands up and walks to the candidate". This gate asks a completely different question:
     * the attempt is already finished, is the result trustworthy in terms of integrity.
     *
     * <p>The two questions came apart exactly when MULTIPLE_PERSONS was lowered from CRITICAL to WARNING
     * (vox-streaming {@code 4cc6598}). The lowering is right on the operational scale: two people in the frame do not
     * necessarily make the proctor run over. But it silently dragged the integrity gate along, and from then on a
     * session with a second person sitting together is NO LONGER sent to a reviewer -- nobody decided
     * that, it fell out of a change to an alert color.
     *
     * <p>Anchoring on TYPE also fixes the time-based crack: {@code level} is stamped at write time and
     * never recomputed, so old records still carry CRITICAL while new records carry WARNING for the same
     * event. Asking by level means two identical attempts get two different outcomes just
     * because they sit on two sides of a deploy. The type has never changed.
     */
    public static final Set<String> INTEGRITY_ALERT_TYPES = Set.of(
        // A second person in the frame: the strongest integrity signal the system has for a SPOKEN assessment --
        // "who is answering" is a question the score cannot answer.
        "MULTIPLE_PERSONS",
        "PHONE_DETECTED",
        "PROHIBITED_OBJECT"
    );

    private UUID id;
    private String eventId;
    private UUID assessmentSessionId;
    private UUID candidateId;
    private String streamId;
    private String streamType;
    private String alertType;
    private String level;
    private String source;
    private String detail;
    private BigDecimal confidence;
    private Long sequenceNo;
    private Instant capturedAt;
    private Instant raisedAt;
    private Instant createdAt;

    public AssessmentProctoringAlert() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public UUID getAssessmentSessionId() {
        return assessmentSessionId;
    }

    public void setAssessmentSessionId(UUID assessmentSessionId) {
        this.assessmentSessionId = assessmentSessionId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(Instant raisedAt) {
        this.raisedAt = raisedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static AssessmentProctoringAlert raised(
            String eventId, UUID assessmentSessionId, UUID candidateId, String streamId, String streamType,
            String alertType, String level, String source, String detail, BigDecimal confidence, Long sequenceNo,
            Instant capturedAt, Instant raisedAt, Instant createdAt) {
        return builder()
            .eventId(eventId).assessmentSessionId(assessmentSessionId).candidateId(candidateId)
            .streamId(streamId).streamType(streamType).alertType(alertType)
            .level(level).source(source).detail(detail)
            .confidence(confidence).sequenceNo(sequenceNo)
            .capturedAt(capturedAt).raisedAt(raisedAt).createdAt(createdAt)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentProctoringAlert target = new AssessmentProctoringAlert();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder eventId(String eventId) {
            target.eventId = eventId;
            return this;
        }

        public Builder assessmentSessionId(UUID assessmentSessionId) {
            target.assessmentSessionId = assessmentSessionId;
            return this;
        }

        public Builder candidateId(UUID candidateId) {
            target.candidateId = candidateId;
            return this;
        }

        public Builder streamId(String streamId) {
            target.streamId = streamId;
            return this;
        }

        public Builder streamType(String streamType) {
            target.streamType = streamType;
            return this;
        }

        public Builder alertType(String alertType) {
            target.alertType = alertType;
            return this;
        }

        public Builder level(String level) {
            target.level = level;
            return this;
        }

        public Builder source(String source) {
            target.source = source;
            return this;
        }

        public Builder detail(String detail) {
            target.detail = detail;
            return this;
        }

        public Builder confidence(BigDecimal confidence) {
            target.confidence = confidence;
            return this;
        }

        public Builder sequenceNo(Long sequenceNo) {
            target.sequenceNo = sequenceNo;
            return this;
        }

        public Builder capturedAt(Instant capturedAt) {
            target.capturedAt = capturedAt;
            return this;
        }

        public Builder raisedAt(Instant raisedAt) {
            target.raisedAt = raisedAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public AssessmentProctoringAlert build() {
            return target;
        }

    }
}
