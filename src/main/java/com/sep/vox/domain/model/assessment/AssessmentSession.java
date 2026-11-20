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

    /**
     * The stream type that has been fixed for this assessment session, null if no stream token has been issued yet.
     *
     * <p>Only meaningful when the assessment sets {@code streamTypePermission = ANY} - then the learner may
     * choose among the types the assessment accepts, and that choice must be recorded instead of only
     * living in the JWT: the token is issued many times in one session (reconnect, renewing the upload
     * credentials), so without storing it every reissue is a chance to switch to a lighter proctoring type
     * in the middle of the assessment. Written once and then locked - see
     * {@code AssessmentSessionRepository#lockChosenStreamType}.
     *
     * <p>It is also the evidence to tell "the learner chose not to stream the screen" from
     * "the screen stream failed": both lead to a missing screen recording in storage.
     */
    private AssessmentRequiredStreamType chosenStreamType;

    /**
     * Checkpoint đồng hồ đếm ngược do client gửi lên, null nếu phiên thi chưa checkpoint lần nào.
     *
     * <p>Phải do client báo chứ server không tự suy ra được: đồng hồ phía WPF <b>không trừ giây khi
     * avatar đang nói</b>, nên thời gian còn lại không phải là {@code startedAt + duration - now}.
     * Chỉ client biết avatar đã nói mất bao lâu.
     *
     * <p>Không có giá trị này thì mỗi lần vào lại phiên thi là đếm ngược lại từ đầu - học viên thoát
     * ra ở phút 25 của bài 30 phút sẽ được thêm trọn 30 phút nữa.
     */
    private Integer remainingSeconds;

    /**
     * Why the most recent AI grading failed; null when the session is not in {@link AssessmentSessionStatus#GRADING_FAILED}.
     *
     * <p>null WHILE failing is also a meaningful answer, not missing data: the DLT branch
     * marks a session failed when the message has run out of retries, and there is no reason to store there. Being able to
     * tell "failed because of X" from "failed, reason unknown" is the whole value of the triage page.
     */
    private String gradingError;

    /** Dịch vụ chấm đã thử bao nhiêu lần trước khi bỏ cuộc; null cùng lúc với {@link #gradingError}. */
    private Integer gradingRetryCount;

    /**
     * Số lần NHÀ TRƯỜNG đã nhờ AI chấm lại phiên này.
     *
     * <p>Khác hẳn {@link #gradingRetryCount}: cột kia là số lần DỊCH VỤ CHẤM tự thử trong một lượt,
     * lấy từ sự kiện Kafka. Cột này đếm số LƯỢT do người ở trường yêu cầu.
     *
     * <p>CHỈ đếm lượt phía trường. Lượt do quản trị hệ thống kích — và sau này là job tự chạy lại khi
     * có sự cố diện rộng — không tiêu vào định mức của trường: bằng không, một lần AI hỏng hàng loạt
     * sẽ âm thầm đốt sạch lượt chấm lại của mọi trường rồi ép hàng trăm bài sang chấm tay, vì một sự
     * cố của chính nền tảng.
     */
    private int schoolRegradeCount;

    public AssessmentSession() {}

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

    public AssessmentSession(UUID assessmentId, UUID candidateId, UUID paperId, Instant startedAt,
            Instant submittedAt, AssessmentSessionStatus status, boolean flagged, String flagReason) {
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

    /**
     * Only set together with {@code setStatus(GRADING_FAILED)}: the constraint
     * {@code chk_assessment_sessions_grading_error_only_when_failed} in the DB rejects a reason on a session that did not fail.
     */
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
