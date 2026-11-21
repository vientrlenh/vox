package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

public class AssessmentItemRuleHit {
    private UUID id;
    private UUID evaluationId;
    private UUID scoringRuleId;
    private String ruleCode; // snapshot of the rule
    private ScoringRuleConditionType conditionType;
    private BigDecimal observedValue; // the observed value (for example 0.7)
    private BigDecimal threshold; // the threshold (for example 0.5)
    private ScoringRuleActionType actionType;
    private String effectSummary; // for example the score dropped from 8.5 to 2.5
    private ScoringRuleSeverity severity;
    private String reasonCode;
    private int appliedOrder;
    private Instant createdAt;
    private UUID createdBy;

    public AssessmentItemRuleHit() {}

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentItemRuleHit target = new AssessmentItemRuleHit();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder evaluationId(UUID evaluationId) {
            target.evaluationId = evaluationId;
            return this;
        }

        public Builder scoringRuleId(UUID scoringRuleId) {
            target.scoringRuleId = scoringRuleId;
            return this;
        }

        public Builder ruleCode(String ruleCode) {
            target.ruleCode = ruleCode;
            return this;
        }

        public Builder conditionType(ScoringRuleConditionType conditionType) {
            target.conditionType = conditionType;
            return this;
        }

        public Builder observedValue(BigDecimal observedValue) {
            target.observedValue = observedValue;
            return this;
        }

        public Builder threshold(BigDecimal threshold) {
            target.threshold = threshold;
            return this;
        }

        public Builder actionType(ScoringRuleActionType actionType) {
            target.actionType = actionType;
            return this;
        }

        public Builder effectSummary(String effectSummary) {
            target.effectSummary = effectSummary;
            return this;
        }

        public Builder severity(ScoringRuleSeverity severity) {
            target.severity = severity;
            return this;
        }

        public Builder reasonCode(String reasonCode) {
            target.reasonCode = reasonCode;
            return this;
        }

        public Builder appliedOrder(int appliedOrder) {
            target.appliedOrder = appliedOrder;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            target.createdBy = createdBy;
            return this;
        }

        public AssessmentItemRuleHit build() {
            return target;
        }

    }
}
