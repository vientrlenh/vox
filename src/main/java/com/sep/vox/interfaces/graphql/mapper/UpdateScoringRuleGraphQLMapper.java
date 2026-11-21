package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.mapper.scoringrule.ScoringRuleParamsMapper;
import com.sep.vox.application.port.input.command.UpdateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.UpdateScoringRuleCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateScoringRuleInput;

public final class UpdateScoringRuleGraphQLMapper {

    private UpdateScoringRuleGraphQLMapper() {
    }

    public static UpdateScoringRuleCommand fromInput(
            ScoringRuleParamsMapper scoringRuleParamsMapper, UUID policyId, UUID ruleId, UpdateScoringRuleInput input) {
        return new UpdateScoringRuleCommand(
                ruleId,
                policyId,
                input.name(),
                input.description(),
                input.conditionType(),
                scoringRuleParamsMapper.fromJsonString(input.conditionParamsJson()),
                input.actionType(),
                scoringRuleParamsMapper.fromJsonString(input.actionParamsJson()),
                input.priority(),
                input.severity(),
                input.stopProcessing(),
                input.isActive()
        );
    }

    public static UpdateSchoolScoringRuleCommand fromSchoolInput(
            ScoringRuleParamsMapper scoringRuleParamsMapper, UUID schoolId, UUID policyId, UUID ruleId,
            UpdateScoringRuleInput input) {
        return new UpdateSchoolScoringRuleCommand(
                schoolId,
                policyId,
                ruleId,
                input.name(),
                input.description(),
                input.conditionType(),
                scoringRuleParamsMapper.fromJsonString(input.conditionParamsJson()),
                input.actionType(),
                scoringRuleParamsMapper.fromJsonString(input.actionParamsJson()),
                input.priority(),
                input.severity(),
                input.stopProcessing(),
                input.isActive()
        );
    }
}
