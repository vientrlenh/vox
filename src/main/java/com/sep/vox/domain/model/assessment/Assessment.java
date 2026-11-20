package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Assessment {
    private UUID id;
    private UUID blueprintId;
    private UUID blueprintVersionId;
    private String code;
    private String name;
    private String description;
    private UUID schoolId;
    private UUID languageId;
    private AssessmentKind kind;
    private AssessmentDeliveryMode deliveryMode;
    private AssessmentStatus status;
    private Integer maxAttempt;
    // H.1: by default computed automatically = MAX(paperDuration) over every AssessmentPaper of the assessment through
    // recalculateAssessmentTimeDuration in the paper create/edit use cases -- but it still accepts the value the
    // user enters when creating the assessment (see CreateAssessmentRequest/CreateClassTestCommand), overwritten as soon as the
    // first paper/section is created.
    private Integer assessmentTimeDurationSecond;
    private ResultDecisionMethod resultDecisionMethod;
    private AssessmentRequiredStreamType requiredStreamType;
    private AssessmentStreamTypePermission streamTypePermission;
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

    public Assessment() {}

    public Assessment(UUID id, UUID blueprintId, UUID blueprintVersionId, String code, String name, String description, UUID schoolId, UUID languageId,
            AssessmentKind kind, AssessmentDeliveryMode deliveryMode, AssessmentStatus status, Integer maxAttempt, Integer assessmentTimeDurationSecond,
            ResultDecisionMethod resultDecisionMethod, AssessmentRequiredStreamType requiredStreamType, AssessmentStreamTypePermission streamTypePermission,
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
        this.assessmentTimeDurationSecond = assessmentTimeDurationSecond;
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

    public Assessment(UUID blueprintId, UUID blueprintVersionId, String code, String name, String description, UUID schoolId, UUID languageId, AssessmentKind kind,
            AssessmentDeliveryMode deliveryMode, AssessmentStatus status, Integer maxAttempt, Integer assessmentTimeDurationSecond, ResultDecisionMethod resultDecisionMethod,
            Instant openAt, Instant closeAt, UUID assessmentPolicyId, boolean requiresOtp, Instant createdAt,AssessmentRequiredStreamType requiredStreamType, AssessmentStreamTypePermission streamTypePermission,
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
        this.assessmentTimeDurationSecond = assessmentTimeDurationSecond;
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
     * H.1: a session must be long enough for the candidate to finish the paper, i.e. (end - start) >= assessmentTimeDurationSecond.
     * When the assessment cannot compute an attempt time yet (no paper variant/questions so null or 0), there is no constraint yet.
     */
    public boolean isScheduleWindowShorterThanAssessmentTime(Instant start, Instant end) {
        if (assessmentTimeDurationSecond == null || assessmentTimeDurationSecond <= 0 || start == null || end == null) {
            return false;
        }
        return Duration.between(start, end).getSeconds() < assessmentTimeDurationSecond;
    }

    /**
     * A session must lie entirely within the open/close window of the assessment: openAt <= start && end <= closeAt.
     * The two bounds are checked independently because an assessment is usually allowed to have openAt/closeAt unset
     * (only CLASS_TEST is required to have them, see UpdateAssessmentStatusUseCase.requireClassTestScheduleWindow).
     */
    public boolean isScheduleWindowOutsideAssessmentWindow(Instant start, Instant end) {
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
        return status == AssessmentStatus.IN_PROGRESS
            || status == AssessmentStatus.CLOSED
            || status == AssessmentStatus.RESULTS_PUBLISHED
            || status == AssessmentStatus.CANCELLED;
    }

    /**
     * An assessment that has closed the books: its attempts can no longer be deleted.
     *
     * <p>Different from {@link #isLockedForEditing()} at both ends, and deliberately so:
     *
     * <ul>
     *   <li>{@code IN_PROGRESS} still allows deletion -- deleting an assessment session exists precisely to rescue a broken
     *       attempt (a faulty room entry, faulty grading) RIGHT DURING the session; locking now would block exactly that way out.
     *   <li>{@code CLOSED} / {@code RESULTS_PUBLISHED} forbid it -- the scores are finalized (and may already be published
     *       to students and used for appeals). Deleting a session here makes the score vanish from the results table
     *       without a trace, and {@code DeleteAssessmentSessionUseCase} is a hard delete that cannot be
     *       undone.
     *   <li>{@code CANCELLED} still allows deletion: for a cancelled assessment, cleaning up junk data is the right thing to do.
     * </ul>
     */
    public boolean isResultsFinalized() {
        return status == AssessmentStatus.CLOSED || status == AssessmentStatus.RESULTS_PUBLISHED;
    }

    /**
     * Whether AssessmentHumanGradingRequired has been published for this assessment. NULL = never reminded.
     *
     * <p>Not in the constructor for the same reason as aiConfidenceThresholdPercent: the constructor
     * of Assessment is already long and has 58 call sites, so adding a parameter means editing all of them just to carry one optional field.
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

    public AssessmentKind getKind() {
        return kind;
    }

    public void setKind(AssessmentKind kind) {
        this.kind = kind;
    }

    public AssessmentDeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(AssessmentDeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentStatus status) {
        this.status = status;
    }

    public Integer getMaxAttempt() {
        return maxAttempt;
    }

    public void setMaxAttempt(Integer maxAttempt) {
        this.maxAttempt = maxAttempt;
    }

    public Integer getAssessmentTimeDurationSecond() {
        return assessmentTimeDurationSecond;
    }

    public void setAssessmentTimeDurationSecond(Integer assessmentTimeDurationSecond) {
        this.assessmentTimeDurationSecond = assessmentTimeDurationSecond;
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

        public AssessmentRequiredStreamType getRequiredStreamType () {
            return requiredStreamType;
        }

        public void setRequiredStreamType (AssessmentRequiredStreamType requiredStreamType){
            this.requiredStreamType = requiredStreamType;
        }

        public AssessmentStreamTypePermission getStreamTypePermission () {
            return streamTypePermission;
        }

        public void setStreamTypePermission (AssessmentStreamTypePermission streamTypePermission){
            this.streamTypePermission = streamTypePermission;
        }


    
    public java.math.BigDecimal getAiConfidenceThresholdPercent() {
        return aiConfidenceThresholdPercent;
    }

    public void setAiConfidenceThresholdPercent(java.math.BigDecimal aiConfidenceThresholdPercent) {
        this.aiConfidenceThresholdPercent = aiConfidenceThresholdPercent;
    }
}
