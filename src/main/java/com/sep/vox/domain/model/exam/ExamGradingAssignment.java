package com.sep.vox.domain.model.exam;

import java.time.Instant;
import java.util.UUID;

/**
 * Phân công một giáo viên chấm tay một bài nộp ({@link ExamCandidateResult}).
 *
 * <p>Một bài chỉ có đúng một giáo viên — uniqueness nằm ở DB trên
 * {@code candidate_result_id}, không phải cặp (bài, giáo viên) như
 * {@link ExamAppealReviewer}. Điểm không nằm ở đây: nó đi thẳng vào
 * {@link ExamItemEvaluation} theo từng phần thi, nên dòng này chỉ giữ trạng thái.
 */
public class ExamGradingAssignment {
    private UUID id;
    private UUID candidateResultId;
    private UUID teacherId;
    private GradingAssignmentStatus status;
    private Instant assignedAt;
    private UUID assignedBy;
    private Instant completedAt;

    public ExamGradingAssignment() {}

    public ExamGradingAssignment(UUID id, UUID candidateResultId, UUID teacherId, GradingAssignmentStatus status,
            Instant assignedAt, UUID assignedBy, Instant completedAt) {
        this.id = id;
        this.candidateResultId = candidateResultId;
        this.teacherId = teacherId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.completedAt = completedAt;
    }

    public ExamGradingAssignment(UUID candidateResultId, UUID teacherId, GradingAssignmentStatus status,
            Instant assignedAt, UUID assignedBy, Instant completedAt) {
        this.candidateResultId = candidateResultId;
        this.teacherId = teacherId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.completedAt = completedAt;
    }

    /** Đã chốt thì không được gỡ, đổi giáo viên hay chấm lại. */
    public boolean isCompleted() {
        return status == GradingAssignmentStatus.COMPLETED;
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

    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }

    public GradingAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(GradingAssignmentStatus status) {
        this.status = status;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
