package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Exam {
    private UUID id;
    private UUID blueprintId;
    private UUID blueprintVersionId;
    private String code;
    private String name;
    private String description;
    private UUID schoolId;
    private UUID languageId;
    private ExamKind kind;
    private ExamDeliveryMode deliveryMode;
    private ExamStatus status;
    private Integer maxAttempt;
    // H.1: mặc định tự động tính = MAX(paperDuration) trên mọi ExamPaper của kỳ thi qua
    // recalculateExamTimeDuration ở các use case tạo/sửa paper -- nhưng vẫn nhận giá trị người
    // dùng nhập lúc tạo exam (xem CreateExamRequest/CreateClassTestCommand), bị ghi đè ngay khi
    // paper/section đầu tiên được tạo.
    private Integer examTimeDurationSecond;
    private ResultDecisionMethod resultDecisionMethod;
    private ExamRequiredStreamType requiredStreamType;
    private ExamStreamTypePermission streamTypePermission;
    private Instant openAt;
    private Instant closeAt;
    private UUID assessmentPolicyId;
    private boolean requiresOtp;

    /**
     * Ngưỡng tin cậy AI do nhà trường đặt cho bài này (0.00-1.00).
     *
     * <p>Bản chấm có {@code overall_confidence} THẤP HƠN ngưỡng sẽ bị đưa sang PENDING_REVIEW
     * thay vì tự công bố. NULL = nhà trường không đặt ngưỡng, chỉ luật cứng trong
     * {@code ConfidenceReviewCalculator} quyết định như trước.
     */
    private BigDecimal aiConfidenceThresholdPercent;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant humanGradingNotifiedAt;

    public Exam() {}

    public Exam(UUID id, UUID blueprintId, UUID blueprintVersionId, String code, String name, String description, UUID schoolId, UUID languageId,
            ExamKind kind, ExamDeliveryMode deliveryMode, ExamStatus status, Integer maxAttempt, Integer examTimeDurationSecond,
            ResultDecisionMethod resultDecisionMethod, ExamRequiredStreamType requiredStreamType, ExamStreamTypePermission streamTypePermission,
            Instant openAt, Instant closeAt, UUID assessmentPolicyId, boolean requiresOtp,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.blueprintId = blueprintId;
        this.blueprintVersionId = blueprintVersionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.kind = kind;
        this.deliveryMode = deliveryMode;
        this.status = status;
        this.maxAttempt = maxAttempt;
        this.examTimeDurationSecond = examTimeDurationSecond;
        this.resultDecisionMethod = resultDecisionMethod;
        this.requiredStreamType = requiredStreamType;
        this.streamTypePermission = streamTypePermission;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.assessmentPolicyId = assessmentPolicyId;
        this.requiresOtp = requiresOtp;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Exam(UUID blueprintId, UUID blueprintVersionId, String code, String name, String description, UUID schoolId, UUID languageId, ExamKind kind,
            ExamDeliveryMode deliveryMode, ExamStatus status, Integer maxAttempt, Integer examTimeDurationSecond, ResultDecisionMethod resultDecisionMethod,
            Instant openAt, Instant closeAt, UUID assessmentPolicyId, boolean requiresOtp, Instant createdAt,ExamRequiredStreamType requiredStreamType, ExamStreamTypePermission streamTypePermission,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.blueprintId = blueprintId;
        this.blueprintVersionId = blueprintVersionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.schoolId = schoolId;
        this.languageId = languageId;
        this.kind = kind;
        this.deliveryMode = deliveryMode;
        this.status = status;
        this.maxAttempt = maxAttempt;
        this.examTimeDurationSecond = examTimeDurationSecond;
        this.resultDecisionMethod = resultDecisionMethod;
        this.requiredStreamType = requiredStreamType;
        this.streamTypePermission = streamTypePermission;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.assessmentPolicyId = assessmentPolicyId;
        this.requiresOtp = requiresOtp;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    /**
     * H.1: một ca thi phải đủ dài để thí sinh làm hết đề, tức là (end - start) >= examTimeDurationSecond.
     * Khi kỳ thi chưa tính được thời gian làm bài (chưa có mã đề/câu hỏi nên null hoặc 0) thì chưa ràng buộc.
     */
    public boolean isScheduleWindowShorterThanExamTime(Instant start, Instant end) {
        if (examTimeDurationSecond == null || examTimeDurationSecond <= 0 || start == null || end == null) {
            return false;
        }
        return Duration.between(start, end).getSeconds() < examTimeDurationSecond;
    }

    /**
     * Ca thi phải nằm trọn trong khung mở/đóng của kỳ thi: openAt <= start && end <= closeAt.
     * Hai cận kiểm tra độc lập nhau vì kỳ thi thường được phép chưa set openAt/closeAt
     * (chỉ CLASS_TEST mới bắt buộc có, xem UpdateExamStatusUseCase.requireClassTestScheduleWindow).
     */
    public boolean isScheduleWindowOutsideExamWindow(Instant start, Instant end) {
        if (start == null || end == null) {
            return false;
        }
        if (openAt != null && start.isBefore(openAt)) {
            return true;
        }
        return closeAt != null && end.isAfter(closeAt);
    }

    /**
     * Từ lúc kỳ thi bắt đầu trở đi thì thông tin kỳ thi và lịch thi (ca thi, giám thị, thí sinh)
     * bị khóa: thí sinh đã vào phòng nên mọi thay đổi đều làm lệch dữ liệu đang chạy. Các thao tác
     * vận hành trong lúc thi (công bố/hoàn thành/huỷ ca, OTP, điểm danh, chấm bài) không nằm trong
     * phạm vi khóa này.
     */
    public boolean isLockedForEditing() {
        return status == ExamStatus.IN_PROGRESS
            || status == ExamStatus.CLOSED
            || status == ExamStatus.RESULTS_PUBLISHED
            || status == ExamStatus.CANCELLED;
    }

    /**
     * Kỳ thi đã chốt sổ: bài thi không còn được xoá nữa.
     *
     * <p>Khác {@link #isLockedForEditing()} ở hai đầu, và cố ý:
     *
     * <ul>
     *   <li>{@code IN_PROGRESS} vẫn cho xoá — xoá phiên thi sinh ra chính là để cứu một lượt thi
     *       hỏng (vào phòng lỗi, chấm lỗi) NGAY TRONG lúc thi; khoá lúc này là bịt đúng đường thoát.
     *   <li>{@code CLOSED} / {@code RESULTS_PUBLISHED} thì cấm — điểm đã chốt (và có thể đã công bố
     *       cho học sinh, đã dùng để phúc khảo). Xoá một phiên ở đây làm điểm biến mất khỏi bảng kết
     *       quả mà không để lại dấu vết, và {@code DeleteExamSessionUseCase} là xoá cứng, không hoàn
     *       tác được.
     *   <li>{@code CANCELLED} vẫn cho xoá: kỳ thi đã huỷ thì dọn dữ liệu rác là việc nên làm.
     * </ul>
     */
    public boolean isResultsFinalized() {
        return status == ExamStatus.CLOSED || status == ExamStatus.RESULTS_PUBLISHED;
    }

    /**
     * Đã phát ExamHumanGradingRequired cho bài thi này chưa. NULL = chưa nhắc lần nào.
     *
     * <p>Không nằm trong constructor vì cùng lý do với aiConfidenceThresholdPercent: constructor
     * của Exam đã dài và có 58 nơi gọi, thêm tham số vào là sửa hết chỉ để mang một trường tuỳ chọn.
     */
    public Instant getHumanGradingNotifiedAt() {
        return humanGradingNotifiedAt;
    }

    public void setHumanGradingNotifiedAt(Instant humanGradingNotifiedAt) {
        this.humanGradingNotifiedAt = humanGradingNotifiedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getLanguageId() {
        return languageId;
    }

    public void setLanguageId(UUID languageId) {
        this.languageId = languageId;
    }

    public ExamKind getKind() {
        return kind;
    }

    public void setKind(ExamKind kind) {
        this.kind = kind;
    }

    public ExamDeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(ExamDeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public Integer getMaxAttempt() {
        return maxAttempt;
    }

    public void setMaxAttempt(Integer maxAttempt) {
        this.maxAttempt = maxAttempt;
    }

    public Integer getExamTimeDurationSecond() {
        return examTimeDurationSecond;
    }

    public void setExamTimeDurationSecond(Integer examTimeDurationSecond) {
        this.examTimeDurationSecond = examTimeDurationSecond;
    }

    public ResultDecisionMethod getResultDecisionMethod() {
        return resultDecisionMethod;
    }

    public void setResultDecisionMethod(ResultDecisionMethod resultDecisionMethod) {
        this.resultDecisionMethod = resultDecisionMethod;
    }

    public Instant getOpenAt() {
        return openAt;
    }

    public void setOpenAt(Instant openAt) {
        this.openAt = openAt;
    }

    public Instant getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(Instant closeAt) {
        this.closeAt = closeAt;
    }

    public UUID getAssessmentPolicyId() {
        return assessmentPolicyId;
    }

    public void setAssessmentPolicyId(UUID assessmentPolicyId) {
        this.assessmentPolicyId = assessmentPolicyId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public UUID getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(UUID blueprintId) {
        this.blueprintId = blueprintId;
    }

    public UUID getBlueprintVersionId() {
        return blueprintVersionId;
    }

    public void setBlueprintVersionId(UUID blueprintVersionId) {
        this.blueprintVersionId = blueprintVersionId;
    }

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }

        public ExamRequiredStreamType getRequiredStreamType () {
            return requiredStreamType;
        }

        public void setRequiredStreamType (ExamRequiredStreamType requiredStreamType){
            this.requiredStreamType = requiredStreamType;
        }

        public ExamStreamTypePermission getStreamTypePermission () {
            return streamTypePermission;
        }

        public void setStreamTypePermission (ExamStreamTypePermission streamTypePermission){
            this.streamTypePermission = streamTypePermission;
        }


    
    public java.math.BigDecimal getAiConfidenceThresholdPercent() {
        return aiConfidenceThresholdPercent;
    }

    public void setAiConfidenceThresholdPercent(java.math.BigDecimal aiConfidenceThresholdPercent) {
        this.aiConfidenceThresholdPercent = aiConfidenceThresholdPercent;
    }
}
