package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Nhật ký đổi trạng thái bài thi. Bảng chỉ-ghi-thêm: không có cột updated_*, không
 * use case nào sửa hay xoá dòng đã ghi.
 *
 * <p>Không đặt CHECK trên {@code from_status}/{@code to_status}: giá trị lấy từ
 * {@code ExamCandidateResultStatus} và enum đó còn thay đổi, mà {@code ddl-auto}
 * không alter được CHECK constraint — ràng buộc cứng ở đây sẽ thành nợ kỹ thuật.
 */
@Entity
@Table(name = "exam_result_status_histories", indexes = {
    @Index(columnList = "candidate_result_id, created_at", name = "idx_result_status_histories_result_time")
})
public class ExamResultStatusHistoryJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "candidate_result_id", nullable = false, updatable = false)
    private UUID candidateResultId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "score_before", precision = 5, scale = 2)
    private BigDecimal scoreBefore;

    @Column(name = "score_after", precision = 5, scale = 2)
    private BigDecimal scoreAfter;

    @Column(name = "source", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_exam_result_status_histories_source_valid",
            constraint = "source IN ('AI_EVALUATION', 'TEACHER_INITIAL', 'TEACHER_SPOT_CHECK', "
                + "'TEACHER_REMEDIATION', 'TEACHER_APPEAL', 'ADMIN_BULK_FINALIZE', 'EXAM_PUBLISH', 'SYSTEM')"
        )
    })
    private String source;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExamResultStatusHistoryJpaEntity() {}

    public ExamResultStatusHistoryJpaEntity(UUID id, UUID candidateResultId, String fromStatus, String toStatus,
            BigDecimal scoreBefore, BigDecimal scoreAfter, String source, UUID actorId, String reason,
            Instant createdAt) {
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

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
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
