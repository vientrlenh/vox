package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.UpdateScoringRuleCommand;
import com.sep.vox.domain.mapper.ScoringRuleParamsMapper;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.interfaces.graphql.dto.request.UpdateScoringRuleInput;

public final class UpdateScoringRuleGraphQLMapper {

    private UpdateScoringRuleGraphQLMapper() {
    }

    public static UpdateScoringRuleCommand fromInput(UUID policyId, UUID ruleId, UpdateScoringRuleInput input) {
        return new UpdateScoringRuleCommand(
                ruleId,
                policyId,
                input.name(),
                input.description(),
                parseConditionType(input.conditionType()),
                ScoringRuleParamsMapper.fromJsonString(input.conditionParamsJson()),
                parseActionType(input.actionType()),
                ScoringRuleParamsMapper.fromJsonString(input.actionParamsJson()),
                input.priority(),
                parseSeverity(input.severity()),
                input.stopProcessing(),
                input.isActive()
        );
    }

    public static UpdateSchoolScoringRuleCommand fromSchoolInput(UUID schoolId, UUID policyId, UUID ruleId, UpdateScoringRuleInput input) {
        return new UpdateSchoolScoringRuleCommand(
                schoolId,
                policyId,
                ruleId,
                input.name(),
                input.description(),
                parseConditionType(input.conditionType()),
                ScoringRuleParamsMapper.fromJsonString(input.conditionParamsJson()),
                parseActionType(input.actionType()),
                ScoringRuleParamsMapper.fromJsonString(input.actionParamsJson()),
                input.priority(),
                parseSeverity(input.severity()),
                input.stopProcessing(),
                input.isActive()
        );
    }

    private static ScoringRuleConditionType parseConditionType(String value) {
        try {
            return ScoringRuleConditionType.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Loại điều kiện (conditionType) không hợp lệ: " + value);
        }
    }

    private static ScoringRuleActionType parseActionType(String value) {
        try {
            return ScoringRuleActionType.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Loại hành động (actionType) không hợp lệ: " + value);
        }
    }

    private static ScoringRuleSeverity parseSeverity(String value) {
        try {
            return ScoringRuleSeverity.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Mức độ nghiêm trọng (severity) không hợp lệ: " + value);
        }
    }
}