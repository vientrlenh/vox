package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentSession {
    private UUID id;
    private UUID assessmentId;
    private UUID candidateId;
    private UUID paperId;
    private Instant startedAt;
    private Instant submittedAt;
    private AssessmentSessionStatus status;
    private boolean flagged;
    private String flagReason;
    private AssessmentRequiredStreamType chosenStreamType;
    private Integer remainingSeconds;
    private String gradingError;
    private Integer gradingRetryCount;
    private int schoolRegradeCount;

    protected AssessmentSession() {}

    public AssessmentSession(UUID id, UUID assessmentId, UUID candidateId, UUID paperId, Instant startedAt,
            Instant submittedAt, AssessmentSessionStatus status, boolean flagged, String flagReason) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.candidateId = candidateId;
        this.paperId = paperId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.status = status;
        this.flagged = flagged;
        this.flagReason = flagReason;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public AssessmentSessionStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentSessionStatus status) {
        this.status = status;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public AssessmentRequiredStreamType getChosenStreamType() {
        return chosenStreamType;
    }

    public void setChosenStreamType(AssessmentRequiredStreamType chosenStreamType) {
        this.chosenStreamType = chosenStreamType;
    }

    public Integer getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Integer remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getGradingError() {
        return gradingError;
    }

    public void setGradingError(String gradingError) {
        this.gradingError = gradingError;
    }

    public Integer getGradingRetryCount() {
        return gradingRetryCount;
    }

    public void setGradingRetryCount(Integer gradingRetryCount) {
        this.gradingRetryCount = gradingRetryCount;
    }

    /**
     * Xóa dấu vết lần chấm hỏng trước. Phải gọi ở MỌI đường rời khỏi {@code GRADING_FAILED}: giữ lại
     * thông điệp cũ thì một phiên đã chấm lại thành công vẫn nằm trong nhóm sự cố trên trang phân
     * loại, và người trực xử lý lại đúng thứ đã xong.
     */
    public int getSchoolRegradeCount() {
        return schoolRegradeCount;
    }

    public void setSchoolRegradeCount(int schoolRegradeCount) {
        this.schoolRegradeCount = schoolRegradeCount;
    }

    /**
     * Trường còn được nhờ AI chấm lại phiên này nữa không.
     *
     * <p>Một lượt: lỗi tạm thời gần như luôn hết ngay ở lần thử đầu, nên lượt thứ hai hỏng tiếp là
     * bằng chứng khá chắc rằng nguyên nhân mang tính tất định (âm thanh hỏng, cấu hình rubric sai) —
     * thử thêm chỉ đốt tiền cho một thất bại đã biết trước. Đường ra lúc đó là giao người chấm.
     */
    public boolean canSchoolRequestAiRegrade() {
        return schoolRegradeCount < MAX_SCHOOL_REGRADES;
    }

    /** Số lượt AI chấm lại tối đa mà phía trường được yêu cầu cho một phiên. */
    public static final int MAX_SCHOOL_REGRADES = 1;

    public void clearGradingFailure() {
        this.gradingError = null;
        this.gradingRetryCount = null;
    }
}
