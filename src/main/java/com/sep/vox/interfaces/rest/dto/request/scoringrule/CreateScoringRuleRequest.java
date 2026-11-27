package com.sep.vox.interfaces.rest.dto.request.scoringrule;

import java.util.Map;

import com.sep.vox.application.port.input.command.scoringrule.CreateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.scoringrule.CreateScoringRuleCommand;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request to create 1 Scoring Rule (automatic scoring rule) for a system Assessment Policy.
// conditionParams/actionParams are free-form JSON, whose structure must match the chosen conditionType/actionType
// (e.g. conditionType = DURATION_LESS_THAN requires conditionParams to have the field "seconds").
public record CreateScoringRuleRequest(

        @NotBlank(message = "Rule code must not be blank")
        String code,

        @NotBlank(message = "Rule name must not be blank")
        String name,

        String description,

        @NotNull(message = "Condition type (conditionType) must not be blank")
        ScoringRuleConditionType conditionType,

        // Example: conditionType=DURATION_LESS_THAN -> {"seconds": 3}
        @NotNull(message = "Condition parameters (conditionParams) must not be blank")
        Map<String, Object> conditionParams,

        @NotNull(message = "Action type (actionType) must not be blank")
        ScoringRuleActionType actionType,

        // Example: actionType=CAP_FINAL_SCORE -> {"maxScore": 0}
        @NotNull(message = "Action parameters (actionParams) must not be blank")
        Map<String, Object> actionParams,

        @Min(value = 1, message = "Priority must be greater than 0")
        int priority,

        @NotNull(message = "Severity must not be blank")
        ScoringRuleSeverity severity,

        // true = if this rule matches, stop right away and do not evaluate the rules with lower priority
        boolean stopProcessing,

        // false = created but not enabled yet, not applied when scoring
        boolean isActive
) {

    public static CreateScoringRuleCommand toCreateScoringRuleCommand(UUID policyId, CreateScoringRuleRequest request) {
        return new CreateScoringRuleCommand(
                policyId,
                request.code(),
                request.name(),
                request.description(),
                request.conditionType(),
                request.conditionParams(),
                request.actionType(),
                request.actionParams(),
                request.priority(),
                request.severity(),
                request.stopProcessing(),
                request.isActive()
        );
    }

    public static CreateSchoolScoringRuleCommand toCreateSchoolScoringRuleCommand(UUID schoolId, UUID policyId, CreateScoringRuleRequest request) {
        return new CreateSchoolScoringRuleCommand(
                schoolId,
                policyId,
                request.code(),
                request.name(),
                request.description(),
                request.conditionType(),
                request.conditionParams(),
                request.actionType(),
                request.actionParams(),
                request.priority(),
                request.severity(),
                request.stopProcessing(),
                request.isActive()
        );
    }
}
