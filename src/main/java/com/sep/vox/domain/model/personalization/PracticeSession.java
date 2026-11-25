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
        return copyBuilder().lastHeartbeatAt(newLastHeartbeatAt).build();
    }

    public PracticeSession withGradedSecondsAndHeartbeat(int newGradedSeconds, Instant newLastHeartbeatAt) {
        return copyBuilder().lastHeartbeatAt(newLastHeartbeatAt).gradedSeconds(newGradedSeconds).build();
    }

    public PracticeSession ended(
            String newStatus,
            String newAbandonDiagnosis,
            int newHelpRequestCount,
            int newLongPauseCount,
            Instant newEndedAt,
            BigDecimal newOverallScore) {
        return copyBuilder()
                .overallScore(newOverallScore)
                .endedAt(newEndedAt)
                .status(newStatus)
                .abandonDiagnosis(newAbandonDiagnosis)
                .helpRequestCount(newHelpRequestCount)
                .longPauseCount(newLongPauseCount)
                .build();
    }

    public PracticeSession closedAsStale(String newStatus, String newAbandonDiagnosis, Instant newEndedAt) {
        return copyBuilder().endedAt(newEndedAt).status(newStatus).abandonDiagnosis(newAbandonDiagnosis).build();
    }

    private Builder copyBuilder() {
        return PracticeSession.builder()
                .id(id)
                .studentId(studentId)
                .practicePaperId(practicePaperId)
                .targetFrameworkBandId(targetFrameworkBandId)
                .chosenPracticeTopicId(chosenPracticeTopicId)
                .targetSubAttributesJson(targetSubAttributesJson)
                .origin(origin)
                .offeredTopicIdsJson(offeredTopicIdsJson)
                .overallScore(overallScore)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .lastHeartbeatAt(lastHeartbeatAt)
                .gradedSeconds(gradedSeconds)
                .status(status)
                .abandonDiagnosis(abandonDiagnosis)
                .helpRequestCount(helpRequestCount)
                .longPauseCount(longPauseCount);
    }

    public static PracticeSession started(UUID studentId, UUID practicePaperId, UUID targetFrameworkBandId,
                                          UUID chosenPracticeTopicId, String origin, String offeredTopicIdsJson,
                                          Instant startedAt) {
        return builder()
                .studentId(studentId)
                .practicePaperId(practicePaperId)
                .targetFrameworkBandId(targetFrameworkBandId)
                .chosenPracticeTopicId(chosenPracticeTopicId)
                .targetSubAttributesJson("[]")
                .origin(origin)
                .offeredTopicIdsJson(offeredTopicIdsJson)
                .startedAt(startedAt)
                .lastHeartbeatAt(startedAt)
                .gradedSeconds(0)
                .status("IN_PROGRESS")
                .helpRequestCount(0)
                .longPauseCount(0)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PracticeSession target = new PracticeSession();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder studentId(UUID studentId) {
            target.studentId = studentId;
            return this;
        }

        public Builder practicePaperId(UUID practicePaperId) {
            target.practicePaperId = practicePaperId;
            return this;
        }

        public Builder targetFrameworkBandId(UUID targetFrameworkBandId) {
            target.targetFrameworkBandId = targetFrameworkBandId;
            return this;
        }

        public Builder chosenPracticeTopicId(UUID chosenPracticeTopicId) {
            target.chosenPracticeTopicId = chosenPracticeTopicId;
            return this;
        }

        public Builder targetSubAttributesJson(String targetSubAttributesJson) {
            target.targetSubAttributesJson = targetSubAttributesJson;
            return this;
        }

        public Builder origin(String origin) {
            target.origin = origin;
            return this;
        }

        public Builder offeredTopicIdsJson(String offeredTopicIdsJson) {
            target.offeredTopicIdsJson = offeredTopicIdsJson;
            return this;
        }

        public Builder overallScore(BigDecimal overallScore) {
            target.overallScore = overallScore;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            target.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(Instant endedAt) {
            target.endedAt = endedAt;
            return this;
        }

        public Builder lastHeartbeatAt(Instant lastHeartbeatAt) {
            target.lastHeartbeatAt = lastHeartbeatAt;
            return this;
        }

        public Builder gradedSeconds(int gradedSeconds) {
            target.gradedSeconds = gradedSeconds;
            return this;
        }

        public Builder status(String status) {
            target.status = status;
            return this;
        }

        public Builder abandonDiagnosis(String abandonDiagnosis) {
            target.abandonDiagnosis = abandonDiagnosis;
            return this;
        }

        public Builder helpRequestCount(int helpRequestCount) {
            target.helpRequestCount = helpRequestCount;
            return this;
        }

        public Builder longPauseCount(int longPauseCount) {
            target.longPauseCount = longPauseCount;
            return this;
        }

        public PracticeSession build() {
            return target;
        }

    }
}
