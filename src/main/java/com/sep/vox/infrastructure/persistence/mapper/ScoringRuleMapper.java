package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import com.sep.vox.infrastructure.persistence.entity.scoringrule.ScoringRuleJpaEntity;

public final class ScoringRuleMapper {

    private ScoringRuleMapper() {}

    public static ScoringRule toDomain(ScoringRuleJpaEntity jpa) {
        ScoringRuleConditionType conditionType = fromConditionType(jpa.getConditionType());
        ScoringRuleActionType actionType = fromActionType(jpa.getActionType());
        return ScoringRule.builder()
            .id(jpa.getId())
            .policyId(jpa.getPolicyId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .description(jpa.getDescription())
            .conditionType(conditionType)
            .conditionParams(JsonValueObjectMapper.fromJson(jpa.getConditionParamsJson(), conditionParamsType(conditionType)))
            .actionType(actionType)
            .actionParams(JsonValueObjectMapper.fromJson(jpa.getActionParamsJson(), actionParamsType(actionType)))
            .priority(jpa.getPriority())
            .severity(fromSeverity(jpa.getSeverity()))
            .stopProcessing(jpa.isStopProcessing())
            .active(jpa.isActive())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static ScoringRuleJpaEntity toJpa(ScoringRule rule) {
        return ScoringRuleJpaEntity.builder()
            .id(rule.getId())
            .policyId(rule.getPolicyId())
            .code(rule.getCode())
            .name(rule.getName())
            .description(rule.getDescription())
            .conditionType(valueOf(rule.getConditionType()))
            .conditionParamsJson(JsonValueObjectMapper.toJson(rule.getConditionParams()))
            .actionType(valueOf(rule.getActionType()))
            .actionParamsJson(JsonValueObjectMapper.toJson(rule.getActionParams()))
            .priority(rule.getPriority())
            .severity(valueOf(rule.getSeverity()))
            .stopProcessing(rule.isStopProcessing())
            .isActive(rule.isActive())
            .createdAt(rule.getCreatedAt())
            .updatedAt(rule.getUpdatedAt())
            .createdBy(rule.getCreatedBy())
            .updatedBy(rule.getUpdatedBy())
            .build();
    }

    private static String valueOf(ScoringRuleSeverity severity) {
        return severity == null ? null : severity.name();
    }

    private static String valueOf(ScoringRuleActionType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ScoringRuleConditionType type) {
        return type == null ? null : type.name();
    }

    private static ScoringRuleConditionType fromConditionType(String value) {
        return value == null ? null : ScoringRuleConditionType.valueOf(value);
    }

    private static ScoringRuleActionType fromActionType(String value) {
        return value == null ? null : ScoringRuleActionType.valueOf(value);
    }

    private static ScoringRuleSeverity fromSeverity(String value) {
        return value == null ? null : ScoringRuleSeverity.valueOf(value);
    }

    private static Class<? extends ScoringRuleConditionParams> conditionParamsType(ScoringRuleConditionType type) {
        return type == null ? null : type.paramTypes();
    }

    private static Class<? extends ScoringRuleActionParams> actionParamsType(ScoringRuleActionType type) {
        return type == null ? null : type.paramType();
    }
}
