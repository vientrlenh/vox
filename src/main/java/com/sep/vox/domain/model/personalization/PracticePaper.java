package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class PracticePaper {

    private UUID id;
    private UUID studentId;
    private UUID practiceTopicId;
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

    public PracticePaper(
            UUID id,
            UUID studentId,
            UUID practiceTopicId,
            String origin,
            String goalAtBuild,
            String offeredTopicIdsJson,
            String previousOfferedTopicIdsJson,
            int plannedSeconds,
            int reservedQuotaSeconds,
            Instant expiresAt,
            String status,
            Instant createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.practiceTopicId = practiceTopicId;
        this.origin = origin;
        this.goalAtBuild = goalAtBuild;
        this.offeredTopicIdsJson = offeredTopicIdsJson;
        this.previousOfferedTopicIdsJson = previousOfferedTopicIdsJson;
        this.plannedSeconds = plannedSeconds;
        this.reservedQuotaSeconds = reservedQuotaSeconds;
        this.expiresAt = expiresAt;
        this.status = status;
        this.createdAt = createdAt;
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
        return new PracticePaper(
            id,
            studentId,
            practiceTopicId,
            origin,
            goalAtBuild,
            offeredTopicIdsJson,
            previousOfferedTopicIdsJson,
            plannedSeconds,
            reservedQuotaSeconds,
            expiresAt,
            newStatus,
            createdAt
        );
    }
}
