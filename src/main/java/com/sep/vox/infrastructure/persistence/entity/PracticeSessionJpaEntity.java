package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "practice_sessions",
    indexes = {
        @Index(name = "idx_practice_session_student_started", columnList = "student_id, started_at"),
        @Index(name = "idx_practice_session_heartbeat", columnList = "status, last_heartbeat_at")
    }
)
public class PracticeSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;
    @Column(name = "practice_paper_id", nullable = false, updatable = false)
    private UUID practicePaperId;
    @Column(name = "target_framework_band_id", nullable = false, updatable = false)
    private UUID targetFrameworkBandId;
    @Column(name = "chosen_practice_topic_id", nullable = false, updatable = false)
    private UUID chosenPracticeTopicId;
    @Column(name = "target_sub_attributes_json", columnDefinition = "TEXT", updatable = false)
    private String targetSubAttributesJson;
    @Column(name = "origin", nullable = false, length = 24, updatable = false)
    private String origin;
    @Column(name = "offered_topic_ids_json", columnDefinition = "TEXT", updatable = false)
    private String offeredTopicIdsJson;
    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
    @Column(name = "graded_seconds", nullable = false)
    private int gradedSeconds;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "abandon_diagnosis", length = 16)
    private String abandonDiagnosis;
    @Column(name = "help_request_count", nullable = false)
    private int helpRequestCount;
    @Column(name = "long_pause_count", nullable = false)
    private int longPauseCount;

    protected PracticeSessionJpaEntity() {
    }

    public PracticeSessionJpaEntity(
            UUID id,
            UUID studentId,
            UUID practicePaperId,
            UUID targetFrameworkBandId,
            UUID chosenPracticeTopicId,
            String targetSubAttributesJson,
            String origin,
            String offeredTopicIdsJson,
            Instant startedAt,
            Instant lastHeartbeatAt,
            int gradedSeconds,
            String status,
            int helpRequestCount,
            int longPauseCount) {
        this.id = id;
        this.studentId = studentId;
        this.practicePaperId = practicePaperId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.chosenPracticeTopicId = chosenPracticeTopicId;
        this.targetSubAttributesJson = targetSubAttributesJson;
        this.origin = origin;
        this.offeredTopicIdsJson = offeredTopicIdsJson;
        this.startedAt = startedAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.gradedSeconds = gradedSeconds;
        this.status = status;
        this.helpRequestCount = helpRequestCount;
        this.longPauseCount = longPauseCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getPracticePaperId() {
        return practicePaperId;
    }


    public UUID getTargetFrameworkBandId() {
        return targetFrameworkBandId;
    }

    public UUID getChosenPracticeTopicId() {
        return chosenPracticeTopicId;
    }

    public String getTargetSubAttributesJson() {
        return targetSubAttributesJson;
    }

    public String getOrigin() {
        return origin;
    }

    public String getOfferedTopicIdsJson() {
        return offeredTopicIdsJson;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public int getGradedSeconds() {
        return gradedSeconds;
    }

    public void setGradedSeconds(int gradedSeconds) {
        this.gradedSeconds = gradedSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAbandonDiagnosis() {
        return abandonDiagnosis;
    }

    public void setAbandonDiagnosis(String abandonDiagnosis) {
        this.abandonDiagnosis = abandonDiagnosis;
    }

    public int getHelpRequestCount() {
        return helpRequestCount;
    }

    public void setHelpRequestCount(int helpRequestCount) {
        this.helpRequestCount = helpRequestCount;
    }

    public int getLongPauseCount() {
        return longPauseCount;
    }

    public void setLongPauseCount(int longPauseCount) {
        this.longPauseCount = longPauseCount;
    }
}
