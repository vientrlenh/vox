package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_item_rule_hits")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentItemRuleHitJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private UUID evaluationId;

    @Column(name = "scoring_rule_id", nullable = false, updatable = false)
    private UUID scoringRuleId;

    @Column(name = "rule_code", nullable = false, updatable = false, length = 255)
    private String ruleCode;

    @Column(name = "condition_type", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String conditionType;

    @Column(name = "observed_value", nullable = false, updatable = false, precision = 3, scale = 2)
    private BigDecimal observedValue;

    @Column(name = "threshold", nullable = false, updatable = false, precision = 3, scale = 2)
    private BigDecimal threshold;

    @Column(name = "action_type", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String actionType;

    @Column(name = "effect_summary", nullable = false, updatable = false, length = 512)
    private String effectSummary;

    @Column(name = "severity", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_item_rule_hits_severity_status", 
            constraint = "severity IN ('INFO', 'WARNING', 'BLOCKING')"
        )
    })
    private String severity;

    @Column(name = "reason_code", nullable = false, updatable = false, length =255)
    private String reasonCode;

    @Column(name = "applied_order", nullable = false, updatable = false)
    private int appliedOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy; 
}
