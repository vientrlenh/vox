package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PracticeSession {

    private UUID id;
    private UUID studentId;
    private UUID practicePaperId;
    private UUID targetFrameworkBandId;
    private UUID chosenPracticeTopicId;
    private String targetSubAttributesJson;
    private String origin;
    private String offeredTopicIdsJson;
    private BigDecimal overallScore;
    private Instant startedAt;
    private Instant endedAt;
    private Instant lastHeartbeatAt;
    private int gradedSeconds;
    private String status;
    private String abandonDiagnosis;
    private int helpRequestCount;
    private int longPauseCount;

    public PracticeSession() {
    }

    public PracticeSession(
            UUID id,
            UUID studentId,
            UUID practicePaperId,
            UUID targetFrameworkBandId,
            UUID chosenPracticeTopicId,
            String targetSubAttributesJson,
            String origin,
            String offeredTopicIdsJson,
            BigDecimal overallScore,
            Instant startedAt,
            Instant endedAt,
            Instant lastHeartbeatAt,
            int gradedSeconds,
            String status,
            String abandonDiagnosis,
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
        this.overallScore = overallScore;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.gradedSeconds = gradedSeconds;
        this.status = status;
        this.abandonDiagnosis = abandonDiagnosis;
        this.helpRequestCount = helpRequestCount;
        this.longPauseCount = longPauseCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getPracticePaperId() {
        return practicePaperId;
    }

    public void setPracticePaperId(UUID practicePaperId) {
        this.practicePaperId = practicePaperId;
    }


    public void setRubricVersionId(UUID rubricVersionId) {
    }

    public UUID getTargetFrameworkBandId() {
        return targetFrameworkBandId;
    }

    public void setTargetFrameworkBandId(UUID targetFrameworkBandId) {
        this.targetFrameworkBandId = targetFrameworkBandId;
    }

    public UUID getChosenPracticeTopicId() {
        return chosenPracticeTopicId;
    }

    public void setChosenPracticeTopicId(UUID chosenPracticeTopicId) {
        this.chosenPracticeTopicId = chosenPracticeTopicId;
    }

    public String getTargetSubAttributesJson() {
        return targetSubAttributesJson;
    }

    public void setTargetSubAttributesJson(String targetSubAttributesJson) {
        this.targetSubAttributesJson = targetSubAttributesJson;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOfferedTopicIdsJson() {
        return offeredTopicIdsJson;
    }

    public void setOfferedTopicIdsJson(String offeredTopicIdsJson) {
        this.offeredTopicIdsJson = offeredTopicIdsJson;
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

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
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

    public PracticeSession withLastHeartbeatAt(Instant newLastHeartbeatAt) {
        return new PracticeSession(
            id, studentId, practicePaperId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            overallScore, startedAt, endedAt, newLastHeartbeatAt, gradedSeconds, status,
            abandonDiagnosis, helpRequestCount, longPauseCount
        );
    }

    public PracticeSession withGradedSecondsAndHeartbeat(int newGradedSeconds, Instant newLastHeartbeatAt) {
        return new PracticeSession(
            id, studentId, practicePaperId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            overallScore, startedAt, endedAt, newLastHeartbeatAt, newGradedSeconds, status,
            abandonDiagnosis, helpRequestCount, longPauseCount
        );
    }

    public PracticeSession ended(
            String newStatus,
            String newAbandonDiagnosis,
            int newHelpRequestCount,
            int newLongPauseCount,
            Instant newEndedAt,
            BigDecimal newOverallScore) {
        return new PracticeSession(
            id, studentId, practicePaperId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            newOverallScore, startedAt, newEndedAt, lastHeartbeatAt, gradedSeconds, newStatus,
            newAbandonDiagnosis, newHelpRequestCount, newLongPauseCount
        );
    }

    public PracticeSession closedAsStale(String newStatus, String newAbandonDiagnosis, Instant newEndedAt) {
        return new PracticeSession(
            id, studentId, practicePaperId, targetFrameworkBandId,
            chosenPracticeTopicId, targetSubAttributesJson, origin, offeredTopicIdsJson,
            overallScore, startedAt, newEndedAt, lastHeartbeatAt, gradedSeconds, newStatus,
            newAbandonDiagnosis, helpRequestCount, longPauseCount
        );
    }
}
