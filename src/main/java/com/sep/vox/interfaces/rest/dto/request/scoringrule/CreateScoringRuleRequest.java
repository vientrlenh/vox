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

        @NotBlank(message = "Mã Rule (code) không được để trống")
        String code,

        @NotBlank(message = "Tên Rule không được để trống")
        String name,

        String description,

        @NotNull(message = "Loại điều kiện (conditionType) không được để trống")
        ScoringRuleConditionType conditionType,

        // Example: conditionType=DURATION_LESS_THAN -> {"seconds": 3}
        @NotNull(message = "Tham số điều kiện (conditionParams) không được để trống")
        Map<String, Object> conditionParams,

        @NotNull(message = "Loại hành động (actionType) không được để trống")
        ScoringRuleActionType actionType,

        // Example: actionType=CAP_FINAL_SCORE -> {"maxScore": 0}
        @NotNull(message = "Tham số hành động (actionParams) không được để trống")
        Map<String, Object> actionParams,

        @Min(value = 1, message = "Độ ưu tiên (priority) phải lớn hơn 0")
        int priority,

        @NotNull(message = "Mức độ nghiêm trọng (severity) không được để trống")
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
