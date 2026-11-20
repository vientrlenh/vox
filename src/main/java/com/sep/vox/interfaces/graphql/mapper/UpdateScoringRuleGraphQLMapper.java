package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.UpdateScoringRuleCommand;
import com.sep.vox.domain.mapper.ScoringRuleParamsMapper;
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
                input.conditionType(),
                ScoringRuleParamsMapper.fromJsonString(input.conditionParamsJson()),
                input.actionType(),
                ScoringRuleParamsMapper.fromJsonString(input.actionParamsJson()),
                input.priority(),
                input.severity(),
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
                input.conditionType(),
                ScoringRuleParamsMapper.fromJsonString(input.conditionParamsJson()),
                input.actionType(),
                ScoringRuleParamsMapper.fromJsonString(input.actionParamsJson()),
                input.priority(),
                input.severity(),
                input.stopProcessing(),
                input.isActive()
        );
    }
}
