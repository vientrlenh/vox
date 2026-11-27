package com.sep.vox.domain.model.scoringrule;

import com.sep.vox.domain.model.BaseModel;
import java.util.UUID;

import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

public class ScoringRule extends BaseModel {
    private UUID policyId;
    private String code;
    private String name;
    private String description;
    private ScoringRuleConditionType conditionType;
    private ScoringRuleConditionParams conditionParams;
    private ScoringRuleActionType actionType;
    private ScoringRuleActionParams actionParams;
    private int priority;
    private ScoringRuleSeverity severity;
    private boolean stopProcessing;
    private boolean isActive;
    
    public ScoringRule() {
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public void setPolicyId(UUID policyId) {
        this.policyId = policyId;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ScoringRuleSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ScoringRuleSeverity severity) {
        this.severity = severity;
    }

    public boolean isStopProcessing() {
        return stopProcessing;
    }

    public void setStopProcessing(boolean stopProcessing) {
        this.stopProcessing = stopProcessing;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public static ScoringRule forPolicy(UUID policyId, String code, String name, String description,
                                        ScoringRuleConditionType conditionType,
                                        ScoringRuleConditionParams conditionParams,
                                        ScoringRuleActionType actionType, ScoringRuleActionParams actionParams,
                                        int priority, ScoringRuleSeverity severity, boolean stopProcessing,
                                        boolean active) {
        return builder()
                .policyId(policyId)
                .code(code)
                .name(name)
                .description(description)
                .conditionType(conditionType)
                .conditionParams(conditionParams)
                .actionType(actionType)
                .actionParams(actionParams)
                .priority(priority)
                .severity(severity)
                .stopProcessing(stopProcessing)
                .active(active)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<ScoringRule, Builder> {

        private Builder() {
            super(new ScoringRule());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder policyId(UUID policyId) {
            target.policyId = policyId;
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

        public Builder conditionType(ScoringRuleConditionType conditionType) {
            target.conditionType = conditionType;
            return this;
        }

        public Builder conditionParams(ScoringRuleConditionParams conditionParams) {
            target.conditionParams = conditionParams;
            return this;
        }

        public Builder actionType(ScoringRuleActionType actionType) {
            target.actionType = actionType;
            return this;
        }

        public Builder actionParams(ScoringRuleActionParams actionParams) {
            target.actionParams = actionParams;
            return this;
        }

        public Builder priority(int priority) {
            target.priority = priority;
            return this;
        }

        public Builder severity(ScoringRuleSeverity severity) {
            target.severity = severity;
            return this;
        }

        public Builder stopProcessing(boolean stopProcessing) {
            target.stopProcessing = stopProcessing;
            return this;
        }

        public Builder active(boolean isActive) {
            target.isActive = isActive;
            return this;
        }
    }
}
