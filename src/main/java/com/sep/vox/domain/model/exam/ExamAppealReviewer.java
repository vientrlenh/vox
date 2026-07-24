package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Phân công một giám khảo vào đơn phúc khảo. Điểm và nhận xét nằm ở
 * {@link ExamAppealReviewerItem} theo từng phần thi — giám khảo chấm tất cả phần
 * của đơn, nên dòng này chỉ giữ trạng thái phân công.
 */
public class ExamAppealReviewer {
    private UUID id;
    private UUID appealId;
    private UUID reviewerId;
    private ExamAppealReviewerStatus status;
    private OffsetDateTime assignedAt;
    private UUID assignedBy;
    private OffsetDateTime submittedAt;

    public ExamAppealReviewer() {}

    public ExamAppealReviewer(UUID id, UUID appealId, UUID reviewerId, ExamAppealReviewerStatus status,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime submittedAt) {
        this.id = id;
        this.appealId = appealId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.submittedAt = submittedAt;
    }

    public ExamAppealReviewer(UUID appealId, UUID reviewerId, ExamAppealReviewerStatus status,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime submittedAt) {
        this.appealId = appealId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public ExamAppealReviewerStatus getStatus() {
        return status;
    }

    public void setStatus(ExamAppealReviewerStatus status) {
        this.status = status;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
