package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;

// DTO trả về cho FE: conditionParams/actionParams được "phẳng hóa" thành Map<String,Object>
// (thay vì trả thẳng Value Object đa hình) để Jackson serialize an toàn, không cần khai báo
// @JsonTypeInfo/@JsonSubTypes cho interface ScoringRuleConditionParams/ScoringRuleActionParams.
public record ScoringRuleDto(
    UUID id,
    UUID assessmentPolicyId,
    String code,
    String name,
    String description,
    ScoringRuleConditionType conditionType,
    ScoringRuleConditionParams conditionParams,
    ScoringRuleActionType actionType,
    ScoringRuleActionParams actionParams,
    Integer priority,
    ScoringRuleSeverity severity,
    Boolean stopProcessing
) {

    public static ScoringRuleDto toDto(ScoringRule scoringRule) {
        return new ScoringRuleDto(
            scoringRule.getId(),
            scoringRule.getAssessmentPolicyId(),
            Code.valueOf(scoringRule.getCode()),
            scoringRule.getName(),
            scoringRule.getDescription(),
            scoringRule.getConditionType(),
            scoringRule.getConditionParams(),
            scoringRule.getActionType(),
            scoringRule.getActionParams(),
            PositiveInteger.valueOf(scoringRule.getPriority()),
            scoringRule.getSeverity(),
            scoringRule.getStopProcessing()
        );
    }
}
