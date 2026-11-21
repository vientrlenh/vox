package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessments", check = {
    @CheckConstraint(
        name = "chk_assessments_required_stream_type_and_permission_valid", 
        constraint = """
            (
                required_stream_type IS NULL 
                AND stream_type_permission IS NULL
            )
            OR 
            (
                required_stream_type = 'CAMERA_AND_SCREEN' 
                AND stream_type_permission IN ('ANY', 'ALL')
            )   
            OR 
            (
                required_stream_type IN ('CAMERA', 'SCREEN')
                AND stream_type_permission IS NULL
            )
        """
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentJpaEntity extends BaseEntity {

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
            name = "chk_assessments_kind_valid",
            constraint = "kind IN ('CENTRALIZED', 'CLASS_TEST')"
        )
    })
    private String kind;

    @Column(name = "delivery_mode", length = 20, check = {
        @CheckConstraint(
            name = "chk_assessments_delivery_mode_valid",
            constraint = "delivery_mode IN ('STUDENT_DEVICE', 'LAB')"
        )
    })
    private String deliveryMode;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessments_status_valid", 
            constraint = "status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'CLOSED', 'RESULTS_PUBLISHED', 'CANCELLED')"
        )
    })
    private String status;

    @Column(name = "max_attempt")
    private Integer maxAttempt;

    // H.1: by default computed automatically = MAX(paperDuration) through recalculateAssessmentTimeDuration, but
    // it still accepts the value the user enters when creating the assessment, overwritten as soon as the first paper/section
    // is created.
    @Column(name = "assessment_time_duration_second")
    private Integer assessmentTimeDurationSecond;

    @Column(name = "result_decision_method", length = 20, check = {
        @CheckConstraint(
            name = "chk_assessments_result_decision_method_valid",
            constraint = "result_decision_method IN ('HIGHEST', 'LATEST', 'AVERAGE', 'FIRST', 'LOWEST')"
        )
    })
    private String resultDecisionMethod;

    @Column(name = "required_stream_type", length = 20, check = {
        @CheckConstraint(
            name = "chk_assessments_required_stream_type_valid", 
            constraint = "required_stream_type IN ('CAMERA', 'SCREEN', 'CAMERA_AND_SCREEN')"
        )
    })
    private String requiredStreamType; 

    @Column(name = "stream_type_permission", length = 20, check = {
        @CheckConstraint(
            name = "chk_assessments_stream_type_permission_valid", 
            constraint = "stream_type_permission IN ('ANY', 'ALL')"
        )
    })
    private String streamTypePermission;

    @Column(name = "open_at")
    private Instant openAt;

    @Column(name = "close_at")
    private Instant closeAt;

    @Column(name = "assessment_policy_id")
    private UUID assessmentPolicyId;

    @Column(name = "requires_otp", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT true")
    private boolean requiresOtp;

    /**
     * AI confidence threshold set by the school for this assessment, in PERCENT (0-100).
     *
     * <p>{@code overall_confidence} is a 0-1 ratio, so it must be MULTIPLIED by 100 before the comparison. A grading below
     * the threshold is sent to PENDING_REVIEW instead of being published automatically. NULL = the school set no
     * threshold, so only the hard rules in {@code ConfidenceReviewCalculator} decide, as before.
     */
    @Column(name = "ai_confidence_threshold_percent", precision = 5, scale = 2)
    private BigDecimal aiConfidenceThresholdPercent;

    @Column(name = "human_grading_notified_at")
    private Instant humanGradingNotifiedAt;
}
