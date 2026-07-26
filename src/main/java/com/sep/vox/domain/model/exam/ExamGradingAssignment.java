package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Phân công một giáo viên chấm tay một bài nộp ({@link ExamCandidateResult}) trong
 * một {@link GradingRoundType vòng chấm}.
 *
 * <p>Một bảng cho cả bốn vòng: chấm lần đầu, hậu kiểm, soi lại bài vô hiệu và phúc
 * khảo. Mỗi bài chỉ có <em>một</em> phân công đang mở tại một thời điểm, nhưng có thể
 * có nhiều dòng đã đóng qua các vòng — bất biến đó nằm ở DB qua unique index trên
 * {@link #activeResultId} (bằng {@code candidateResultId} khi ASSIGNED, {@code null}
 * khi COMPLETED; Postgres coi các NULL là khác nhau).
 *
 * <p>Điểm không nằm ở đây: nó đi thẳng vào {@link ExamItemEvaluation} theo từng phần
 * thi. Dòng này giữ trạng thái, kết luận ({@link GradingOutcome}) và điểm trước khi
 * chấm để đo độ lệch.
 */
public class ExamGradingAssignment {
    private UUID id;
    private UUID candidateResultId;
    private UUID teacherId;
    private GradingRoundType roundType;
    /** Chỉ có giá trị khi {@code roundType = APPEAL}. */
    private UUID appealId;
    private GradingAssignmentStatus status;
    private GradingOutcome outcome;
    /** Tổng điểm của bài lúc được giao — mốc so lệch sau khi chấm. */
    private BigDecimal scoreBefore;
    private OffsetDateTime assignedAt;
    private UUID assignedBy;
    private OffsetDateTime completedAt;
    private OffsetDateTime deadlineAt;
    /** Đã gửi mail nhắc hạn lúc nào; null = chưa nhắc. Chống nhắc trùng. */
    private OffsetDateTime remindedAt;
    /** Bắt buộc khi outcome là INVALIDATED / CLEARED_INVALID / DECLINED. */
    private String reason;
    private UUID activeResultId;
    private Integer version;

    public ExamGradingAssignment() {}

    public ExamGradingAssignment(UUID id, UUID candidateResultId, UUID teacherId, GradingRoundType roundType,
            UUID appealId, GradingAssignmentStatus status, GradingOutcome outcome, BigDecimal scoreBefore,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime completedAt, OffsetDateTime deadlineAt,
            OffsetDateTime remindedAt, String reason, UUID activeResultId, Integer version) {
        this.id = id;
        this.candidateResultId = candidateResultId;
        this.teacherId = teacherId;
        this.roundType = roundType;
        this.appealId = appealId;
        this.status = status;
        this.outcome = outcome;
        this.scoreBefore = scoreBefore;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.completedAt = completedAt;
        this.deadlineAt = deadlineAt;
        this.remindedAt = remindedAt;
        this.reason = reason;
        this.activeResultId = activeResultId;
        this.version = version;
    }

    /**
     * Mở một vòng chấm mới. {@code activeResultId} được set bằng chính bài — đây là
     * chỗ duy nhất dựng bất biến "một phân công mở / bài", nên đừng tự new rồi set tay.
     */
    public static ExamGradingAssignment open(UUID candidateResultId, UUID teacherId, GradingRoundType roundType,
            UUID appealId, BigDecimal scoreBefore, OffsetDateTime assignedAt, UUID assignedBy,
            OffsetDateTime deadlineAt) {
        var assignment = new ExamGradingAssignment();
        assignment.candidateResultId = candidateResultId;
        assignment.teacherId = teacherId;
        assignment.roundType = roundType;
        assignment.appealId = appealId;
        assignment.status = GradingAssignmentStatus.ASSIGNED;
        assignment.scoreBefore = scoreBefore;
        assignment.assignedAt = assignedAt;
        assignment.assignedBy = assignedBy;
        assignment.deadlineAt = deadlineAt;
        assignment.activeResultId = candidateResultId;
        return assignment;
    }

    /** Đã chốt thì không được gỡ, đổi giáo viên hay chấm lại. */
    public boolean isCompleted() {
        return status == GradingAssignmentStatus.COMPLETED;
    }

    /**
     * Đóng phân công kèm kết luận. Nhả {@code activeResultId} về null để bài nhận
     * được vòng tiếp theo mà unique index vẫn giữ nguyên ý nghĩa.
     */
    public void complete(GradingOutcome outcome, String reason, OffsetDateTime at) {
        this.status = GradingAssignmentStatus.COMPLETED;
        this.outcome = outcome;
        this.reason = reason;
        this.completedAt = at;
        this.activeResultId = null;
    }

    public boolean isOverdue(OffsetDateTime now) {
        return !isCompleted() && deadlineAt != null && deadlineAt.isBefore(now);
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

    public GradingRoundType getRoundType() {
        return roundType;
    }

    public void setRoundType(GradingRoundType roundType) {
        this.roundType = roundType;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public GradingAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(GradingAssignmentStatus status) {
        this.status = status;
    }

    public GradingOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(GradingOutcome outcome) {
        this.outcome = outcome;
    }

    public BigDecimal getScoreBefore() {
        return scoreBefore;
    }

    public void setScoreBefore(BigDecimal scoreBefore) {
        this.scoreBefore = scoreBefore;
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

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(OffsetDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public OffsetDateTime getRemindedAt() {
        return remindedAt;
    }

    public void setRemindedAt(OffsetDateTime remindedAt) {
        this.remindedAt = remindedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getActiveResultId() {
        return activeResultId;
    }

    public void setActiveResultId(UUID activeResultId) {
        this.activeResultId = activeResultId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
