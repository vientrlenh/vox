package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import com.sep.vox.infrastructure.persistence.entity.ScoringRuleJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ScoringRuleMapper {

    private ScoringRuleMapper() {}

    public static ScoringRule toDomain(ScoringRuleJpaEntity jpa) {
        try {
            ScoringRuleConditionType conditionType = ScoringRuleConditionType.from(jpa.getConditionType());
            ScoringRuleActionType actionType = ScoringRuleActionType.from(jpa.getActionType());

            return ScoringRule.builder()
                .id(jpa.getId())
                .assessmentPolicyId(jpa.getAssessmentPolicyId())
                .code(Code.from(jpa.getCode()))
                .name(jpa.getName())
                .description(jpa.getDescription())
                .conditionType(conditionType)
                .conditionParams(conditionParamsFromJson(conditionType, jpa.getConditionParamsJson()))
                .actionType(actionType)
                .actionParams(actionParamsFromJson(actionType, jpa.getActionParamsJson()))
                .priority(PositiveInteger.from(jpa.getPriority()))
                .severity(ScoringRuleSeverity.from(jpa.getSeverity()))
                .stopProcessing(jpa.getStopProcessing())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping scoring rule entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static ScoringRuleJpaEntity toJpa(ScoringRule rule) {
        try {
            return ScoringRuleJpaEntity.builder()
                .id(rule.getId())
                .assessmentPolicyId(rule.getAssessmentPolicyId())
                .code(Code.valueOf(rule.getCode()))
                .name(rule.getName())
                .description(rule.getDescription())
                .conditionType(ScoringRuleConditionType.value(rule.getConditionType()))
                .conditionParamsJson(paramsToJson(rule.getConditionParams()))
                .actionType(ScoringRuleActionType.value(rule.getActionType()))
                .actionParamsJson(paramsToJson(rule.getActionParams()))
                .priority(PositiveInteger.valueOf(rule.getPriority()))
                .severity(ScoringRuleSeverity.value(rule.getSeverity()))
                .stopProcessing(rule.getStopProcessing())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping scoring rule model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }

    private static ScoringRuleConditionParams conditionParamsFromJson(ScoringRuleConditionType type, String json) {
        return type == null || json == null ? null : JsonValueObjectMapper.fromJson(json, type.paramTypes());
    }

    private static ScoringRuleActionParams actionParamsFromJson(ScoringRuleActionType type, String json) {
        return type == null || json == null ? null : JsonValueObjectMapper.fromJson(json, type.paramType());
    }

    private static String paramsToJson(Object params) {
        return params == null ? null : JsonValueObjectMapper.toJson(params);
    }
}
