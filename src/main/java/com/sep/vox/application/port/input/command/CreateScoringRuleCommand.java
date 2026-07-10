package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// Lệnh tạo mới 1 Scoring Rule (luật chấm điểm tự động) cho 1 Assessment Policy hệ thống.
// conditionParams/actionParams giữ nguyên dạng Map thô (JSON) từ Request, UseCase sẽ tự
// convert sang Value Object cụ thể dựa theo conditionType/actionType đã chọn.
public record CreateScoringRuleCommand(
        UUID policyId,
        String code,
        String name,
        String description,
        ScoringRuleConditionType conditionType,
        Map<String, Object> conditionParams,
        ScoringRuleActionType actionType,
        Map<String, Object> actionParams,
        int priority,
        ScoringRuleSeverity severity,
        boolean stopProcessing,
        boolean isActive
) {
}