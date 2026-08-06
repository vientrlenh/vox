package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "practice_paper",
    indexes = {
        @Index(
            name = "idx_practice_paper_student_created",
            columnList = "student_id, created_at"
        ),
        @Index(
            name = "idx_practice_paper_target_band",
            columnList = "target_framework_band_id"
        )
    }
)
public class PracticePaperJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    /** Bậc học sinh chọn cho phiên. Nullable: đề dựng trước V15 không mang lựa chọn nào. */
    @Column(name = "target_framework_band_id", updatable = false)
    private UUID targetFrameworkBandId;

    @Column(name = "origin", nullable = false, length = 24, updatable = false)
    private String origin;

    @Column(name = "goal_type", nullable = false, length = 24, updatable = false)
    private String goalType;

    @Column(name = "offered_topic_ids_json", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String offeredTopicIdsJson;

    @Column(name = "previous_offered_topic_ids_json", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String previousOfferedTopicIdsJson;

    @Column(name = "planned_seconds", nullable = false, updatable = false)
    private int plannedSeconds;

    @Column(name = "reserved_quota_seconds", nullable = false, updatable = false)
    private int reservedQuotaSeconds;

    @Column(name = "reservation_expires_at", nullable = false, updatable = false)
    private Instant reservationExpiresAt;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PracticePaperJpaEntity() {
    }

    public PracticePaperJpaEntity(
            UUID id,
            UUID studentId,
            UUID practiceTopicId,
            UUID targetFrameworkBandId,
            String origin,
            String goalType,
            String offeredTopicIdsJson,
            String previousOfferedTopicIdsJson,
            int plannedSeconds,
            int reservedQuotaSeconds,
            Instant reservationExpiresAt,
            String status,
            Instant createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.practiceTopicId = practiceTopicId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.origin = origin;
        this.goalType = goalType;
        this.offeredTopicIdsJson = offeredTopicIdsJson;
        this.previousOfferedTopicIdsJson = previousOfferedTopicIdsJson;
        this.plannedSeconds = plannedSeconds;
        this.reservedQuotaSeconds = reservedQuotaSeconds;
        this.reservationExpiresAt = reservationExpiresAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getPracticeTopicId() {
        return practiceTopicId;
    }

    public UUID getTargetFrameworkBandId() {
        return targetFrameworkBandId;
    }

    public String getOrigin() {
        return origin;
    }

    public String getGoalType() {
        return goalType;
    }

    public String getOfferedTopicIdsJson() {
        return offeredTopicIdsJson;
    }

    public String getPreviousOfferedTopicIdsJson() {
        return previousOfferedTopicIdsJson;
    }

    public int getPlannedSeconds() {
        return plannedSeconds;
    }

    public int getReservedQuotaSeconds() {
        return reservedQuotaSeconds;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
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
}
