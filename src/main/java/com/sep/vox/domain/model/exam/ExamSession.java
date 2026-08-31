package com.sep.vox.domain.model.exam;

import java.time.Instant;
import java.util.UUID;

public class ExamSession {
    private UUID id;
    private UUID examId;
    private UUID candidateId;
    private UUID paperId;
    private Instant startedAt;
    private Instant submittedAt;
    private ExamSessionStatus status;
    private boolean flagged;
    private String flagReason;

    /**
     * Loại stream đã được chốt cho phiên thi này, null nếu chưa phát token stream lần nào.
     *
     * <p>Chỉ có ý nghĩa khi kỳ thi đặt {@code streamTypePermission = ANY} - lúc đó học viên được
     * tự chọn trong số các loại mà kỳ thi chấp nhận, và lựa chọn ấy phải được ghi lại thay vì chỉ
     * sống trong JWT: token được phát lại nhiều lần trong một phiên (reconnect, gia hạn credential
     * upload), nên nếu không lưu thì mỗi lần phát lại là một cơ hội đổi sang loại giám sát nhẹ hơn
     * giữa kỳ thi. Ghi một lần rồi khóa - xem
     * {@code ExamSessionRepository#lockChosenStreamType}.
     *
     * <p>Đồng thời đây là bằng chứng để phân biệt "học viên chọn không stream màn hình" với
     * "stream màn hình bị lỗi": cả hai đều dẫn tới việc thiếu bản ghi screen trên storage.
     */
    private ExamRequiredStreamType chosenStreamType;

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
     * Vì sao lần chấm AI gần nhất hỏng; null khi phiên không ở {@link ExamSessionStatus#GRADING_FAILED}.
     *
     * <p>null KHI ĐANG lỗi cũng là một câu trả lời có nghĩa, không phải dữ liệu thiếu: nhánh DLT
     * đánh dấu phiên hỏng khi bản tin đã hết đường retry, và ở đó không có lý do nào để lưu. Phân
     * biệt được "hỏng vì X" với "hỏng, không rõ vì sao" là toàn bộ giá trị của trang phân loại.
     */
    private String gradingError;

    /** Dịch vụ chấm đã thử bao nhiêu lần trước khi bỏ cuộc; null cùng lúc với {@link #gradingError}. */
    private Integer gradingRetryCount;

    public ExamSession() {}

    public ExamSession(UUID id, UUID examId, UUID candidateId, UUID paperId, Instant startedAt,
            Instant submittedAt, ExamSessionStatus status, boolean flagged, String flagReason) {
        this.id = id;
        this.examId = examId;
        this.candidateId = candidateId;
        this.paperId = paperId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.status = status;
        this.flagged = flagged;
        this.flagReason = flagReason;
    }

    public ExamSession(UUID examId, UUID candidateId, UUID paperId, Instant startedAt,
            Instant submittedAt, ExamSessionStatus status, boolean flagged, String flagReason) {
        this.examId = examId;
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

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
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

    public ExamSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ExamSessionStatus status) {
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

    public ExamRequiredStreamType getChosenStreamType() {
        return chosenStreamType;
    }

    public void setChosenStreamType(ExamRequiredStreamType chosenStreamType) {
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

    /** Chỉ đặt cùng lúc với {@code setStatus(GRADING_FAILED)}: ràng buộc
     * {@code chk_exam_sessions_grading_error_only_when_failed} ở DB từ chối lý do trên phiên không lỗi. */
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
    public void clearGradingFailure() {
        this.gradingError = null;
        this.gradingRetryCount = null;
    }
}
