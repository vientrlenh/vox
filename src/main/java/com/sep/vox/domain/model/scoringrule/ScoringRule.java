package com.sep.vox.domain.model.scoringrule;

import java.util.UUID;

import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

public class ScoringRule {
    private UUID id;
    private UUID assessmentPolicyId;
    private Code code;
    private String name;
    private String description;
    private ScoringRuleConditionType conditionType;
    private ScoringRuleConditionParams conditionParams;
    private ScoringRuleActionType actionType;
    private ScoringRuleActionParams actionParams;
    private PositiveInteger priority;
    private ScoringRuleSeverity severity;
    private Boolean stopProcessing;
    
    protected ScoringRule() {
    }

    public ScoringRule(UUID id, UUID assessmentPolicyId, Code code, String name, String description,
            ScoringRuleConditionType conditionType, ScoringRuleConditionParams conditionParams,
            ScoringRuleActionType actionType, ScoringRuleActionParams actionParams, PositiveInteger priority, ScoringRuleSeverity severity, Boolean stopProcessing) {
        this.id = id;
        this.assessmentPolicyId = assessmentPolicyId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionParams = conditionParams;
        this.actionType = actionType;
        this.actionParams = actionParams;
        this.priority = priority;
        this.severity = severity;
        this.stopProcessing = stopProcessing;
    }

    public ScoringRule(Builder builder) {
        this.id = builder.id;
        this.assessmentPolicyId = builder.assessmentPolicyId;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.conditionType = builder.conditionType;
        this.conditionParams = builder.conditionParams;
        this.actionType = builder.actionType;
        this.actionParams = builder.actionParams;
        this.priority = builder.priority;
        this.severity = builder.severity;
        this.stopProcessing = builder.stopProcessing;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentPolicyId() {
        return assessmentPolicyId;
    }

    public void setAssessmentPolicyId(UUID assessmentPolicyId) {
        this.assessmentPolicyId = assessmentPolicyId;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
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

    public ScoringRuleConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ScoringRuleConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public ScoringRuleConditionParams getConditionParams() {
        return conditionParams;
    }

    public void setConditionParams(ScoringRuleConditionParams conditionParams) {
        this.conditionParams = conditionParams;
    }

    public ScoringRuleActionType getActionType() {
        return actionType;
    }

    public void setActionType(ScoringRuleActionType actionType) {
        this.actionType = actionType;
    }

    public ScoringRuleActionParams getActionParams() {
        return actionParams;
    }

    public void setActionParams(ScoringRuleActionParams actionParams) {
        this.actionParams = actionParams;
    }

    public PositiveInteger getPriority() {
        return priority;
    }

    public void setPriority(PositiveInteger priority) {
        this.priority = priority;
    }


    public ScoringRuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ScoringRuleSeverity severity) {
        this.severity = severity;
    }

    public Boolean getStopProcessing() {
        return stopProcessing;
    }

    public void setStopProcessing(Boolean stopProcessing) {
        this.stopProcessing = stopProcessing;
    }

    public static Builder builder() {
        return new ScoringRule.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID assessmentPolicyId;
        private Code code;
        private String name;
        private String description;
        private ScoringRuleConditionType conditionType;
        private ScoringRuleConditionParams conditionParams;
        private ScoringRuleActionType actionType;
        private ScoringRuleActionParams actionParams;
        private PositiveInteger priority;
        private ScoringRuleSeverity severity;
        private Boolean stopProcessing;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder assessmentPolicyId(UUID assessmentPolicyId) {
            this.assessmentPolicyId = assessmentPolicyId;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder conditionType(ScoringRuleConditionType conditionType) {
            this.conditionType = conditionType;
            return this;
        }

        public Builder conditionParams(ScoringRuleConditionParams conditionParams) {
            this.conditionParams = conditionParams;
            return this;
        }

        public Builder actionType(ScoringRuleActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder actionParams(ScoringRuleActionParams actionParams) {
            this.actionParams = actionParams;
            return this;
        }

        public Builder priority(PositiveInteger priority) {
            this.priority = priority;
            return this;
        }

        public Builder severity(ScoringRuleSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder stopProcessing(Boolean stopProcessing) {
            this.stopProcessing = stopProcessing;
            return this;
        }

        public ScoringRule build() {
            return new ScoringRule(this);
        }
    }
}
