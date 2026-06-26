package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
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
@Table(name = "exam_item_rule_hits")
public class ExamItemRuleHitJpaEntity {

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
            name = "chk_exam_item_rule_hits_severity_status", 
            constraint = "severity IN ('INFO', 'WARNING', 'BLOCKING')"
        )
    })
    private String severity;

    @Column(name = "reason_code", nullable = false, updatable = false, length =255)
    private String reasonCode;

    @Column(name = "applied_order", nullable = false, updatable = false)
    private int appliedOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy; 

    protected ExamItemRuleHitJpaEntity() {}

    public ExamItemRuleHitJpaEntity(UUID id, UUID evaluationId, UUID scoringRuleId, String ruleCode,
            String conditionType, BigDecimal observedValue, BigDecimal threshold, String actionType,
            String effectSummary, String severity, String reasonCode, int appliedOrder, OffsetDateTime createdAt,
            UUID createdBy) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.scoringRuleId = scoringRuleId;
        this.ruleCode = ruleCode;
        this.conditionType = conditionType;
        this.observedValue = observedValue;
        this.threshold = threshold;
        this.actionType = actionType;
        this.effectSummary = effectSummary;
        this.severity = severity;
        this.reasonCode = reasonCode;
        this.appliedOrder = appliedOrder;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }

    public UUID getScoringRuleId() {
        return scoringRuleId;
    }

    public void setScoringRuleId(UUID scoringRuleId) {
        this.scoringRuleId = scoringRuleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public BigDecimal getObservedValue() {
        return observedValue;
    }

    public void setObservedValue(BigDecimal observedValue) {
        this.observedValue = observedValue;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getEffectSummary() {
        return effectSummary;
    }

    public void setEffectSummary(String effectSummary) {
        this.effectSummary = effectSummary;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public int getAppliedOrder() {
        return appliedOrder;
    }

    public void setAppliedOrder(int appliedOrder) {
        this.appliedOrder = appliedOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    
}
