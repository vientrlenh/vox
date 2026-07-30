package com.sep.vox.domain.model.exam;

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
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

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


    }

