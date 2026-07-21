package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exams")
public class ExamJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "blueprint_id")
    private UUID blueprintId;

    @Column(name = "blueprint_version_id")
    private UUID blueprintVersionId;
    
    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "kind", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exams_kind_valid",
            constraint = "kind IN ('CENTRALIZED', 'CLASS_TEST')"
        )
    })
    private String kind;

    @Column(name = "delivery_mode", length = 20, check = {
        @CheckConstraint(
            name = "chk_exams_delivery_mode_valid",
            constraint = "delivery_mode IN ('STUDENT_DEVICE', 'LAB')"
        )
    })
    private String deliveryMode;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exams_status_valid", 
            constraint = "status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'CLOSED', 'RESULTS_PUBLISHED', 'CANCELLED')"
        )
    })
    private String status;

    @Column(name = "max_attempt")
    private Integer maxAttempt;

    @Column(name = "result_decision_method", length = 20, check = {
        @CheckConstraint(
            name = "chk_exams_result_decision_method_valid",
            constraint = "result_decision_method IN ('HIGHEST', 'LATEST', 'AVERAGE', 'FIRST', 'LOWEST')"
        )
    })
    private String resultDecisionMethod;

    @Column(name = "open_at")
    private OffsetDateTime openAt;

    @Column(name = "close_at")
    private OffsetDateTime closeAt;

    @Column(name = "assessment_policy_id")
    private UUID assessmentPolicyId;

    @Column(name = "requires_otp", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT true")
    private boolean requiresOtp;

    // H.1: tự động tính = MAX(paperDuration) trên mọi ExamPaper - không có input frontend nào.
    @Column(name = "exam_time_duration_second")
    private Integer examTimeDurationSecond;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ExamJpaEntity() {}

    public ExamJpaEntity(UUID id, UUID blueprintId, UUID blueprintVersionId, String code, String name, String description, UUID schoolId, UUID languageId,
            String kind, String deliveryMode, String status, Integer maxAttempt, String resultDecisionMethod,
            OffsetDateTime openAt, OffsetDateTime closeAt, UUID assessmentPolicyId, boolean requiresOtp,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
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
        this.resultDecisionMethod = resultDecisionMethod;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public Integer getMaxAttempt() {
        return maxAttempt;
    }

    public void setMaxAttempt(Integer maxAttempt) {
        this.maxAttempt = maxAttempt;
    }

    public String getResultDecisionMethod() {
        return resultDecisionMethod;
    }

    public void setResultDecisionMethod(String resultDecisionMethod) {
        this.resultDecisionMethod = resultDecisionMethod;
    }

    public OffsetDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(OffsetDateTime openAt) {
        this.openAt = openAt;
    }

    public OffsetDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(OffsetDateTime closeAt) {
        this.closeAt = closeAt;
    }

    public UUID getAssessmentPolicyId() {
        return assessmentPolicyId;
    }

    public void setAssessmentPolicyId(UUID assessmentPolicyId) {
        this.assessmentPolicyId = assessmentPolicyId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
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

    public Integer getExamTimeDurationSecond() {
        return examTimeDurationSecond;
    }

    public void setExamTimeDurationSecond(Integer examTimeDurationSecond) {
        this.examTimeDurationSecond = examTimeDurationSecond;
    }
}
