package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "scoring_rules", indexes = {
    @Index(columnList = "assessment_policy_id, code", name = "idx_scoring_rules_policy_code", unique = true),
    @Index(columnList = "assessment_policy_id, priority", name = "idx_scoring_rules_policy_priority")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ScoringRuleJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_policy_id", nullable = false, updatable = false)
    private UUID assessmentPolicyId;

    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "condition_type", nullable = false, length = 50, check = {
        @CheckConstraint(
            name = "chk_scoring_rules_condition_type_valid",
            constraint = "condition_type IN ('DURATION_LESS_THAN', 'DURATION_GREATER_THAN', 'WORD_COUNT_LESS_THAN', 'WORD_COUNT_GREATER_THAN', 'CRITERION_SCORE_LESS_THAN', 'CRITERION_SCORE_GREATER_THAN', 'FINAL_SCORE_LESS_THAN', 'FINAL_SCORE_GREATER_THAN', 'TASK_RELEVANCE_LESS_THAN', 'OFF_TOPIC_RATIO_GREATER_THAN', 'CODE_SWITCHING_RATIO_GREATER_THAN', 'ASR_CONFIDENCE_LESS_THAN', 'AI_CONFIDENCE_LESS_THAN', 'AUDIO_QUALITY_LESS_THAN', 'SILENCE_RATIO_GREATER_THAN', 'SPEECH_RATE_LESS_THAN', 'SPEECH_RATE_GREATER_THAN')"
        )
    })
    private String conditionType;

    @Column(name = "condition_params_json", nullable = false, columnDefinition = "TEXT")
    private String conditionParamsJson;

    @Column(name = "action_type", nullable = false, length = 50, check = {
        @CheckConstraint(
            name = "chk_scoring_rules_action_type_valid",
            constraint = "action_type IN ('CAP_FINAL_SCORE', 'CAP_CRITERION_SCORE', 'ADD_FINAL_SCORE_DELTA', 'ADD_CRITERION_SCORE_DELTA', 'CAP_FRAMEWORK_RESULT_BAND', 'SET_FRAMEWORK_RESULT_BAND', 'REQUIRE_HUMAN_REVIEW', 'MARK_RESPONSE_INVALID', 'REQUIRE_RETAKE', 'ADD_FEEDBACK_TAG', 'ADD_REVIEW_REASON')"
        )
    })
    private String actionType;

    @Column(name = "action_params_json", nullable = false, columnDefinition = "TEXT")
    private String actionParamsJson;

    @Column(name = "priority", nullable = false, check = {
        @CheckConstraint(
            name = "chk_scoring_rules_priority_positive",
            constraint = "priority > 0"
        )
    })
    private Integer priority;

    @Column(name = "severity", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_scoring_rules_severity_valid",
            constraint = "severity IN ('INFO', 'WARNING', 'BLOCKING')"
        )
    })
    private String severity;

    @Column(name = "stop_processing", nullable = false)
    private Boolean stopProcessing;

}
