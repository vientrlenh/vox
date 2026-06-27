package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

public class ExamItemRuleHit {
    private UUID id;
    private UUID evaluationId;
    private UUID scoringRuleId;
    private String ruleCode; // snapshot của rule
    private ScoringRuleConditionType conditionType;
    private BigDecimal observedValue; // giá trị đạt được (ví dụ 0.7)
    private BigDecimal threshold; // ngưỡng (ví dụ 0.5)
    private ScoringRuleActionType actionType;
    private String effectSummary; // ví dụ điểm từ 8.5 xuống 2.5
    private ScoringRuleSeverity severity;
    private String reasonCode;
    private int appliedOrder;
    private OffsetDateTime createdAt;
    private UUID createdBy;

    public ExamItemRuleHit() {}

    public ExamItemRuleHit(UUID id, UUID evaluationId, UUID scoringRuleId, String ruleCode,
            ScoringRuleConditionType conditionType, BigDecimal observedValue, BigDecimal threshold,
            ScoringRuleActionType actionType, String effectSummary, ScoringRuleSeverity severity, String reasonCode,
            int appliedOrder, OffsetDateTime createdAt, UUID createdBy) {
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

    public ExamItemRuleHit(UUID evaluationId, UUID scoringRuleId, String ruleCode,
            ScoringRuleConditionType conditionType, BigDecimal observedValue, BigDecimal threshold,
            ScoringRuleActionType actionType, String effectSummary, ScoringRuleSeverity severity, String reasonCode,
            int appliedOrder, OffsetDateTime createdAt, UUID createdBy) {
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

    public ScoringRuleConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ScoringRuleConditionType conditionType) {
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

    public ScoringRuleActionType getActionType() {
        return actionType;
    }

    public void setActionType(ScoringRuleActionType actionType) {
        this.actionType = actionType;
    }

    public String getEffectSummary() {
        return effectSummary;
    }

    public void setEffectSummary(String effectSummary) {
        this.effectSummary = effectSummary;
    }

    public ScoringRuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ScoringRuleSeverity severity) {
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
