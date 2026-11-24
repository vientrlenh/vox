package com.sep.vox.interfaces.graphql.input.scoringrule;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.mapper.scoringrule.ScoringRuleParamsMapper;
import com.sep.vox.application.port.input.command.scoringrule.UpdateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.scoringrule.UpdateScoringRuleCommand;

// GraphQL input to update a Scoring Rule. There is no "code" field because the code is immutable.
// conditionParamsJson/actionParamsJson are raw JSON strings (the schema has no Map/JSON scalar),
// and their structure must match the chosen conditionType/actionType - for example:
//   conditionType = "DURATION_LESS_THAN"  -> conditionParamsJson = "{\"seconds\": 3}"
//   actionType     = "CAP_FINAL_SCORE"    -> actionParamsJson    = "{\"maxScore\": 0}"
public record UpdateScoringRuleInput(
        String name,
        ArgumentValue<String> description,
        ScoringRuleConditionType conditionType,
        String conditionParamsJson,
        ScoringRuleActionType actionType,
        String actionParamsJson,
        int priority,
        ScoringRuleSeverity severity,
        boolean stopProcessing,
        boolean isActive
) {

    public static UpdateScoringRuleCommand toUpdateScoringRuleCommand(
            ScoringRuleParamsMapper scoringRuleParamsMapper, UUID policyId, UUID ruleId, UpdateScoringRuleInput input) {
        return new UpdateScoringRuleCommand(
                ruleId,
                policyId,
                input.name(),
                input.description().value(), !input.description().isOmitted(),
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

    public static UpdateSchoolScoringRuleCommand toUpdateSchoolScoringRuleCommand(
            ScoringRuleParamsMapper scoringRuleParamsMapper, UUID schoolId, UUID policyId, UUID ruleId,
            UpdateScoringRuleInput input) {
        return new UpdateSchoolScoringRuleCommand(
                schoolId,
                policyId,
                ruleId,
                input.name(),
                input.description().value(), !input.description().isOmitted(),
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
