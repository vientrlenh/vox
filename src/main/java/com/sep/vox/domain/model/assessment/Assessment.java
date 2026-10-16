package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class Assessment extends BaseModel {
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
    private Integer timeDurationSecond;
    private AssessmentResultDecisionMethod resultDecisionMethod;
    private AssessmentRequiredStreamType requiredStreamType;
    private AssessmentStreamTypePermission streamTypePermission;
    private Instant openAt;
    private Instant closeAt;
    private UUID assessmentPolicyId;
    private boolean requiresOtp;
    private BigDecimal aiConfidenceThresholdPercent;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant humanGradingNotifiedAt;

    protected Assessment() {}

    public Assessment(UUID blueprintId, UUID blueprintVersionId, String code, String name, String description, UUID schoolId, UUID languageId,
            AssessmentKind kind, AssessmentDeliveryMode deliveryMode, AssessmentStatus status, Integer maxAttempt, Integer timeDurationSecond,
            AssessmentResultDecisionMethod resultDecisionMethod, AssessmentRequiredStreamType requiredStreamType, AssessmentStreamTypePermission streamTypePermission,
            Instant openAt, Instant closeAt, UUID assessmentPolicyId, boolean requiresOtp,
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
        this.timeDurationSecond = timeDurationSecond;
        this.resultDecisionMethod = resultDecisionMethod;
        this.requiredStreamType = requiredStreamType;
        this.streamTypePermission = streamTypePermission;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.assessmentPolicyId = assessmentPolicyId;
        this.requiresOtp = requiresOtp;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
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

    public Integer getTimeDurationSecond() {
        return timeDurationSecond;
    }

    public void setTimeDurationSecond(Integer timeDurationSecond) {
        this.timeDurationSecond = timeDurationSecond;
    }

    public AssessmentResultDecisionMethod getResultDecisionMethod() {
        return resultDecisionMethod;
    }

    public void setResultDecisionMethod(AssessmentResultDecisionMethod resultDecisionMethod) {
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

     public boolean isScheduleWindowShorterThanAssessmentTime(Instant start, Instant end) {
        if (timeDurationSecond == null || timeDurationSecond <= 0 || start == null || end == null) {
            return false;
        }
        return Duration.between(start, end).getSeconds() < timeDurationSecond;
    }

    public boolean isScheduleWindowOutsideAssessmentWindow(Instant start, Instant end) {
        if (start == null || end == null) {
            return false;
        }
        if (openAt != null && start.isBefore(openAt)) {
            return true;
        }
        return closeAt != null && end.isAfter(closeAt);
    }

    public boolean isLockedForEditing() {
        return status == AssessmentStatus.IN_PROGRESS
            || status == AssessmentStatus.CLOSED
            || status == AssessmentStatus.RESULTS_PUBLISHED
            || status == AssessmentStatus.CANCELLED;
    }

    public boolean isResultsFinalized() {
        return status == AssessmentStatus.CLOSED || status == AssessmentStatus.RESULTS_PUBLISHED;
    }

    public Instant getHumanGradingNotifiedAt() {
        return humanGradingNotifiedAt;
    }

    public void setHumanGradingNotifiedAt(Instant humanGradingNotifiedAt) {
        this.humanGradingNotifiedAt = humanGradingNotifiedAt;
    }
    
    public java.math.BigDecimal getAiConfidenceThresholdPercent() {
        return aiConfidenceThresholdPercent;
    }

    public void setAiConfidenceThresholdPercent(java.math.BigDecimal aiConfidenceThresholdPercent) {
        this.aiConfidenceThresholdPercent = aiConfidenceThresholdPercent;
    }
}
