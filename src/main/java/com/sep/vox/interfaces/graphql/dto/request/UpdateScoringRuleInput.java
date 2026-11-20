package com.sep.vox.interfaces.graphql.dto.request;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// GraphQL input to update a Scoring Rule. There is no "code" field because the code is immutable.
// conditionParamsJson/actionParamsJson are raw JSON strings (the schema has no Map/JSON scalar),
// and their structure must match the chosen conditionType/actionType - for example:
//   conditionType = "DURATION_LESS_THAN"  -> conditionParamsJson = "{\"seconds\": 3}"
//   actionType     = "CAP_FINAL_SCORE"    -> actionParamsJson    = "{\"maxScore\": 0}"
public record UpdateScoringRuleInput(
        String name,
        String description,
        ScoringRuleConditionType conditionType,
        String conditionParamsJson,
        ScoringRuleActionType actionType,
        String actionParamsJson,
        int priority,
        ScoringRuleSeverity severity,
        boolean stopProcessing,
        boolean isActive
) {
}