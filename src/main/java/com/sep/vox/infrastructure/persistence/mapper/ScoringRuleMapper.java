package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import com.sep.vox.infrastructure.persistence.entity.ScoringRuleJpaEntity;

public final class ScoringRuleMapper {

    private ScoringRuleMapper() {}

    public static ScoringRule toDomain(ScoringRuleJpaEntity jpa) {
        var conditionType = fromConditionType(jpa.getConditionType());
        var actionType = fromActionType(jpa.getActionType());
        return new ScoringRule(
            jpa.getId(),
            jpa.getPolicyId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            conditionType,
            JsonValueObjectMapper.fromJson(jpa.getConditionParamsJson(), conditionParamsType(conditionType)),
            actionType,
            JsonValueObjectMapper.fromJson(jpa.getActionParamsJson(), actionParamsType(actionType)),
            jpa.getPriority(),
            fromSeverity(jpa.getSeverity()),
            jpa.isStopProcessing(),
            jpa.isActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ScoringRuleJpaEntity toJpa(ScoringRule rule) {
        return new ScoringRuleJpaEntity(
            rule.getId(),
            rule.getPolicyId(),
            rule.getCode(),
            rule.getName(),
            rule.getDescription(),
            valueOf(rule.getConditionType()),
            JsonValueObjectMapper.toJson(rule.getConditionParams()),
            valueOf(rule.getActionType()),
            JsonValueObjectMapper.toJson(rule.getActionParams()),
            rule.getPriority(),
            valueOf(rule.getSeverity()),
            rule.isStopProcessing(),
            rule.isActive(),
            rule.getCreatedAt(),
            rule.getUpdatedAt(),
            rule.getCreatedBy(),
            rule.getUpdatedBy()
        );
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
