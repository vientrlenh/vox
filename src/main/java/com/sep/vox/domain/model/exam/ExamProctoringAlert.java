package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Một đợt vi phạm được phát hiện trong lúc giám sát thi, do vox-streaming phát trên topic
 * {@code exam.alert.raised}.
 *
 * <p>Đây là sổ append-only: không sửa, không xoá theo nghiệp vụ. Lý do là mục đích dùng của nó --
 * bằng chứng cho quyết định về tính hợp lệ của bài thi. Một bản ghi bằng chứng có thể sửa được thì
 * không còn là bằng chứng.
 *
 * <p><b>Không</b> tham gia vào việc tính điểm. Cảnh báo là phát hiện của AI kèm độ tin cậy, chạy
 * trên webcam, có dương tính giả; còn điểm tiêu chí đo năng lực ngôn ngữ. Hai thứ nằm trên hai trục
 * khác nhau, và để cảnh báo kéo điểm xuống nghĩa là phạt nghi vấn gian lận bằng cách giả vờ rằng
 * học viên nói kém hơn thực tế -- vừa sai với học viên, vừa không kiểm toán được. Cần cẩu đúng là
 * quyết định uphold/regrade/invalidate, và cờ nghi vấn trên phiên thi.
 */
public class ExamProctoringAlert {

    /**
     * Những loại cảnh báo đặt câu hỏi "bài này có phải do CHÍNH thí sinh làm không" -- và vì thế
     * buộc bài phải qua tay người soát. Xem {@code RecordExamAttemptEvaluationUseCase}.
     *
     * <p><b>Theo LOẠI, không theo {@code level}.</b> Trước đây cửa này hỏi {@code level = 'CRITICAL'},
     * và đó là dùng một cái thang để trả lời hai câu hỏi khác nhau. Thang mức của vox-streaming
     * ({@code DefaultAlertLevel}) định nghĩa theo VIỆC GIÁM THỊ PHẢI LÀM NGAY TRONG LÚC THI --
     * "CRITICAL: giám thị đứng dậy đi tới chỗ thí sinh". Còn cửa này hỏi một câu hoàn toàn khác:
     * bài đã thi xong rồi, kết quả có đáng tin về mặt liêm chính không.
     *
     * <p>Hai câu đó tách nhau ra đúng lúc MULTIPLE_PERSONS bị hạ từ CRITICAL xuống WARNING
     * (vox-streaming {@code 4cc6598}). Việc hạ mức là đúng trên thang thao tác: hai người trong khung
     * không nhất thiết bắt giám thị chạy tới. Nhưng nó lặng lẽ kéo theo cửa liêm chính, và từ đó một
     * phiên có người thứ hai ngồi cùng KHÔNG còn bị đẩy sang người soát nữa -- không ai quyết định
     * điều đó, nó rơi ra từ một thay đổi về màu sắc cảnh báo.
     *
     * <p>Bám vào LOẠI còn chữa luôn vết nứt theo thời gian: {@code level} được đóng dấu lúc ghi và
     * không bao giờ tính lại, nên bản ghi cũ vẫn mang CRITICAL còn bản ghi mới mang WARNING cho cùng
     * một sự việc. Hỏi theo level nghĩa là hai bài thi giống hệt nhau nhận hai kết cục khác nhau chỉ
     * vì chúng nằm hai bên một lần deploy. Loại thì chưa từng đổi.
     */
    public static final Set<String> INTEGRITY_ALERT_TYPES = Set.of(
        // Người thứ hai trong khung: tín hiệu liêm chính mạnh nhất mà hệ có với một bài thi NÓI --
        // câu hỏi "ai đang trả lời" là câu hỏi mà điểm số không trả lời được.
        "MULTIPLE_PERSONS",
        "PHONE_DETECTED",
        "PROHIBITED_OBJECT"
    );

    private UUID id;
    private String eventId;
    private UUID examSessionId;
    private UUID candidateId;
    private String streamId;
    private String streamType;
    private String alertType;
    private String level;
    private String source;
    private String detail;
    private BigDecimal confidence;
    private Long sequenceNo;
    private Instant capturedAt;
    private Instant raisedAt;
    private Instant createdAt;

    public ExamProctoringAlert() {
    }

    public ExamProctoringAlert(UUID id, String eventId, UUID examSessionId, UUID candidateId, String streamId,
            String streamType, String alertType, String level, String source, String detail, BigDecimal confidence,
            Long sequenceNo, Instant capturedAt, Instant raisedAt, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.examSessionId = examSessionId;
        this.candidateId = candidateId;
        this.streamId = streamId;
        this.streamType = streamType;
        this.alertType = alertType;
        this.level = level;
        this.source = source;
        this.detail = detail;
        this.confidence = confidence;
        this.sequenceNo = sequenceNo;
        this.capturedAt = capturedAt;
        this.raisedAt = raisedAt;
        this.createdAt = createdAt;
    }

    public ExamProctoringAlert(String eventId, UUID examSessionId, UUID candidateId, String streamId,
            String streamType, String alertType, String level, String source, String detail, BigDecimal confidence,
            Long sequenceNo, Instant capturedAt, Instant raisedAt, Instant createdAt) {
        this(null, eventId, examSessionId, candidateId, streamId, streamType, alertType, level, source, detail,
                confidence, sequenceNo, capturedAt, raisedAt, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(Instant raisedAt) {
        this.raisedAt = raisedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
