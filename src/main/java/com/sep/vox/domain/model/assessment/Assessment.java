package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

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
     * AI confidence threshold set by the school for this assessment (0.00-1.00).
     *
     * <p>A grading whose {@code overall_confidence} is BELOW the threshold is sent to PENDING_REVIEW
     * instead of being published automatically. NULL = the school set no threshold, so only the hard rules in
     * {@code ConfidenceReviewCalculator} decide, as before.
     */
    private BigDecimal aiConfidenceThresholdPercent;
    private Instant humanGradingNotifiedAt;

    public Assessment() {}

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
     * From the moment the assessment starts, the assessment information and the schedule (schedules, proctors,
     * candidates) are locked: candidates have already entered the room, so any change would skew the running data.
     * Operations performed during the assessment (publish/complete/cancel a schedule, OTP, attendance, grading) are not
     * covered by this lock.
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

    public static Assessment centralizedDraft(
            UUID blueprintId, String code, String name, String description, UUID schoolId, UUID languageId) {
        // A centralized assessment is taken on school devices by default; a school that lets candidates
        // bring their own device into the room picks STUDENT_DEVICE when creating it.
        return draft(AssessmentKind.CENTRALIZED, AssessmentDeliveryMode.LAB, true, code, name, description, schoolId,
                languageId)
            .blueprintId(blueprintId)
            .build();
    }

    public static Assessment classTestDraft(
            String code, String name, String description, UUID schoolId, UUID languageId) {
        // A class test is taken right in the room; the only difference is the device used: the school's machine (LAB)
        // or the one the student brings (STUDENT_DEVICE, the default). It goes straight in without OTP by default; the
        // teacher turns OTP on to tighten it like a centralized assessment.
        return draft(AssessmentKind.CLASS_TEST, AssessmentDeliveryMode.STUDENT_DEVICE, false, code, name, description,
                schoolId, languageId)
            .build();
    }

    private static Builder draft(
            AssessmentKind kind, AssessmentDeliveryMode defaultDeliveryMode, boolean defaultRequiresOtp, String code,
            String name, String description, UUID schoolId, UUID languageId) {
        return builder()
            .code(code).name(name).description(description).schoolId(schoolId).languageId(languageId)
            .kind(kind).status(AssessmentStatus.DRAFT)
            .deliveryMode(defaultDeliveryMode).requiresOtp(defaultRequiresOtp)
            .maxAttempt(1).resultDecisionMethod(ResultDecisionMethod.HIGHEST);
    }

    /** Applies the optional settings of a new assessment: a null argument keeps the default set by the draft factory. */
    public void configure(
            AssessmentDeliveryMode deliveryMode, Integer maxAttempt, Integer assessmentTimeDurationSecond,
            ResultDecisionMethod resultDecisionMethod, Boolean requiresOtp) {
        this.deliveryMode = Objects.requireNonNullElse(deliveryMode, this.deliveryMode);
        this.maxAttempt = Objects.requireNonNullElse(maxAttempt, this.maxAttempt);
        this.assessmentTimeDurationSecond = assessmentTimeDurationSecond;
        this.resultDecisionMethod = Objects.requireNonNullElse(resultDecisionMethod, this.resultDecisionMethod);
        this.requiresOtp = Objects.requireNonNullElse(requiresOtp, this.requiresOtp);
    }

    public void scheduleBetween(Instant openAt, Instant closeAt, UUID assessmentPolicyId) {
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.assessmentPolicyId = assessmentPolicyId;
    }

    public void useStreamProctoring(
            AssessmentRequiredStreamType requiredStreamType, AssessmentStreamTypePermission streamTypePermission,
            BigDecimal aiConfidenceThresholdPercent) {
        this.requiredStreamType = requiredStreamType;
        this.streamTypePermission = streamTypePermission;
        this.aiConfidenceThresholdPercent = aiConfidenceThresholdPercent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<Assessment, Builder> {

        private Builder() {
            super(new Assessment());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder blueprintId(UUID blueprintId) {
            target.blueprintId = blueprintId;
            return this;
        }

        public Builder blueprintVersionId(UUID blueprintVersionId) {
            target.blueprintVersionId = blueprintVersionId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder languageId(UUID languageId) {
            target.languageId = languageId;
            return this;
        }

        public Builder kind(AssessmentKind kind) {
            target.kind = kind;
            return this;
        }

        public Builder deliveryMode(AssessmentDeliveryMode deliveryMode) {
            target.deliveryMode = deliveryMode;
            return this;
        }

        public Builder status(AssessmentStatus status) {
            target.status = status;
            return this;
        }

        public Builder maxAttempt(Integer maxAttempt) {
            target.maxAttempt = maxAttempt;
            return this;
        }

        public Builder assessmentTimeDurationSecond(Integer assessmentTimeDurationSecond) {
            target.assessmentTimeDurationSecond = assessmentTimeDurationSecond;
            return this;
        }

        public Builder resultDecisionMethod(ResultDecisionMethod resultDecisionMethod) {
            target.resultDecisionMethod = resultDecisionMethod;
            return this;
        }

        public Builder requiredStreamType(AssessmentRequiredStreamType requiredStreamType) {
            target.requiredStreamType = requiredStreamType;
            return this;
        }

        public Builder streamTypePermission(AssessmentStreamTypePermission streamTypePermission) {
            target.streamTypePermission = streamTypePermission;
            return this;
        }

        public Builder openAt(Instant openAt) {
            target.openAt = openAt;
            return this;
        }

        public Builder closeAt(Instant closeAt) {
            target.closeAt = closeAt;
            return this;
        }

        public Builder assessmentPolicyId(UUID assessmentPolicyId) {
            target.assessmentPolicyId = assessmentPolicyId;
            return this;
        }

        public Builder requiresOtp(boolean requiresOtp) {
            target.requiresOtp = requiresOtp;
            return this;
        }

        public Builder aiConfidenceThresholdPercent(BigDecimal aiConfidenceThresholdPercent) {
            target.aiConfidenceThresholdPercent = aiConfidenceThresholdPercent;
            return this;
        }

        public Builder humanGradingNotifiedAt(Instant humanGradingNotifiedAt) {
            target.humanGradingNotifiedAt = humanGradingNotifiedAt;
            return this;
        }

    }
}
