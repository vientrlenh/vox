package com.sep.vox.domain.model.scoringrule;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;



public class ScoringRule {
    private UUID id;
    private UUID policyId;
    private String code;
    private String name;
    private String description;
    private ScoringRuleConditionType conditionType;
    private ScoringRuleConditionParams conditionParams;
    private ScoringRuleActionType actionType;
    private ScoringRuleActionParams actionParams;
    private int priority;
    private ScoringRuleAppliesTo appliesTo;
    private ScoringRuleSeverity severity;
    private boolean stopProcessing;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    public ScoringRule() {
    }

    public ScoringRule(UUID id, UUID policyId, String code, String name, String description,
            ScoringRuleConditionType conditionType, ScoringRuleConditionParams conditionParams,
            ScoringRuleActionType actionType, ScoringRuleActionParams actionParams, int priority,
            ScoringRuleAppliesTo appliesTo, ScoringRuleSeverity severity, boolean stopProcessing, boolean isActive,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.policyId = policyId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionParams = conditionParams;
        this.actionType = actionType;
        this.actionParams = actionParams;
        this.priority = priority;
        this.appliesTo = appliesTo;
        this.severity = severity;
        this.stopProcessing = stopProcessing;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ScoringRule(UUID policyId, String code, String name, String description,
            ScoringRuleConditionType conditionType, ScoringRuleConditionParams conditionParams,
            ScoringRuleActionType actionType, ScoringRuleActionParams actionParams, int priority,
            ScoringRuleAppliesTo appliesTo, ScoringRuleSeverity severity, boolean stopProcessing, boolean isActive,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.policyId = policyId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionParams = conditionParams;
        this.actionType = actionType;
        this.actionParams = actionParams;
        this.priority = priority;
        this.appliesTo = appliesTo;
        this.severity = severity;
        this.stopProcessing = stopProcessing;
        this.isActive = isActive;
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

    public ScoringRuleAppliesTo getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(ScoringRuleAppliesTo appliesTo) {
        this.appliesTo = appliesTo;
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

    
}
