package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class PracticePaper {

    private UUID id;
    private UUID studentId;
    private UUID practiceTopicId;
    /** The band the student CHOSE for this session -- null for papers built before the band-selection screen existed. */
    private UUID targetFrameworkBandId;
    private String origin;
    private String goalAtBuild;
    private String offeredTopicIdsJson;
    private String previousOfferedTopicIdsJson;
    private int plannedSeconds;
    private int reservedQuotaSeconds;
    private Instant expiresAt;
    private String status;
    private Instant createdAt;

    public PracticePaper() {
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

    public UUID getPracticeTopicId() {
        return practiceTopicId;
    }

    public void setPracticeTopicId(UUID practiceTopicId) {
        this.practiceTopicId = practiceTopicId;
    }

    public UUID getTargetFrameworkBandId() {
        return targetFrameworkBandId;
    }

    public void setTargetFrameworkBandId(UUID targetFrameworkBandId) {
        this.targetFrameworkBandId = targetFrameworkBandId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getGoalAtBuild() {
        return goalAtBuild;
    }

    public void setGoalAtBuild(String goalAtBuild) {
        this.goalAtBuild = goalAtBuild;
    }

    public String getOfferedTopicIdsJson() {
        return offeredTopicIdsJson;
    }

    public void setOfferedTopicIdsJson(String offeredTopicIdsJson) {
        this.offeredTopicIdsJson = offeredTopicIdsJson;
    }

    public String getPreviousOfferedTopicIdsJson() {
        return previousOfferedTopicIdsJson;
    }

    public void setPreviousOfferedTopicIdsJson(String previousOfferedTopicIdsJson) {
        this.previousOfferedTopicIdsJson = previousOfferedTopicIdsJson;
    }

    public int getPlannedSeconds() {
        return plannedSeconds;
    }

    public void setPlannedSeconds(int plannedSeconds) {
        this.plannedSeconds = plannedSeconds;
    }

    public int getReservedQuotaSeconds() {
        return reservedQuotaSeconds;
    }

    public void setReservedQuotaSeconds(int reservedQuotaSeconds) {
        this.reservedQuotaSeconds = reservedQuotaSeconds;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PracticePaper withStatus(String newStatus) {
        return PracticePaper.builder()
                .id(id)
                .studentId(studentId)
                .practiceTopicId(practiceTopicId)
                .targetFrameworkBandId(targetFrameworkBandId)
                .origin(origin)
                .goalAtBuild(goalAtBuild)
                .offeredTopicIdsJson(offeredTopicIdsJson)
                .previousOfferedTopicIdsJson(previousOfferedTopicIdsJson)
                .plannedSeconds(plannedSeconds)
                .reservedQuotaSeconds(reservedQuotaSeconds)
                .expiresAt(expiresAt)
                .status(newStatus)
                .createdAt(createdAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PracticePaper target = new PracticePaper();

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

        public Builder practiceTopicId(UUID practiceTopicId) {
            target.practiceTopicId = practiceTopicId;
            return this;
        }

        public Builder targetFrameworkBandId(UUID targetFrameworkBandId) {
            target.targetFrameworkBandId = targetFrameworkBandId;
            return this;
        }

        public Builder origin(String origin) {
            target.origin = origin;
            return this;
        }

        public Builder goalAtBuild(String goalAtBuild) {
            target.goalAtBuild = goalAtBuild;
            return this;
        }

        public Builder offeredTopicIdsJson(String offeredTopicIdsJson) {
            target.offeredTopicIdsJson = offeredTopicIdsJson;
            return this;
        }

        public Builder previousOfferedTopicIdsJson(String previousOfferedTopicIdsJson) {
            target.previousOfferedTopicIdsJson = previousOfferedTopicIdsJson;
            return this;
        }

        public Builder plannedSeconds(int plannedSeconds) {
            target.plannedSeconds = plannedSeconds;
            return this;
        }

        public Builder reservedQuotaSeconds(int reservedQuotaSeconds) {
            target.reservedQuotaSeconds = reservedQuotaSeconds;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            target.expiresAt = expiresAt;
            return this;
        }

        public Builder status(String status) {
            target.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public PracticePaper build() {
            return target;
        }

    }
}
