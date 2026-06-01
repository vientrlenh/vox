package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "scoring_rules", indexes = {
    @Index(columnList = "policy_id, code", name = "idx_scoring_rules_policy_code", unique = true)
})

public class ScoringRuleJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(name = "id", nullable = false, updatable = false, insertable = false, columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "policy_id", nullable = false, updatable = false)
    private UUID policyId;

    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    @Column(name = "condition_params_json", nullable = false, columnDefinition = "TEXT")
    private String conditionParamsJson;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "action_params_json", nullable = false, columnDefinition = "TEXT")
    private String actionParamsJson;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "applies_to", nullable = false, length = 30)
    private String appliesTo;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "stop_processing", nullable = false)
    private boolean stopProcessing;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;


    protected ScoringRuleJpaEntity() {}


    public ScoringRuleJpaEntity(UUID id, UUID policyId, String code, String name, String description,
            String conditionType, String conditionParamsJson, String actionType, String actionParamsJson, int priority,
            String appliesTo, String severity, boolean stopProcessing, boolean isActive, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.policyId = policyId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionParamsJson = conditionParamsJson;
        this.actionType = actionType;
        this.actionParamsJson = actionParamsJson;
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


    public ScoringRuleJpaEntity(UUID policyId, String code, String name, String description, String conditionType,
            String conditionParamsJson, String actionType, String actionParamsJson, int priority, String appliesTo,
            String severity, boolean stopProcessing, boolean isActive, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.policyId = policyId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionParamsJson = conditionParamsJson;
        this.actionType = actionType;
        this.actionParamsJson = actionParamsJson;
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


    public String getConditionType() {
        return conditionType;
    }


    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }


    public String getConditionParamsJson() {
        return conditionParamsJson;
    }


    public void setConditionParamsJson(String conditionParamsJson) {
        this.conditionParamsJson = conditionParamsJson;
    }


    public String getActionType() {
        return actionType;
    }


    public void setActionType(String actionType) {
        this.actionType = actionType;
    }


    public String getActionParamsJson() {
        return actionParamsJson;
    }


    public void setActionParamsJson(String actionParamsJson) {
        this.actionParamsJson = actionParamsJson;
    }


    public int getPriority() {
        return priority;
    }


    public void setPriority(int priority) {
        this.priority = priority;
    }


    public String getAppliesTo() {
        return appliesTo;
    }


    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }


    public String getSeverity() {
        return severity;
    }


    public void setSeverity(String severity) {
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
