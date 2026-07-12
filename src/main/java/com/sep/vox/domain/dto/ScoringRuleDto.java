package com.sep.vox.domain.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// DTO trả về cho FE: conditionParams/actionParams được "phẳng hóa" thành Map<String,Object>
// (thay vì trả thẳng Value Object đa hình) để Jackson serialize an toàn, không cần khai báo
// @JsonTypeInfo/@JsonSubTypes cho interface ScoringRuleConditionParams/ScoringRuleActionParams.
public record ScoringRuleDto(
        UUID id,
        UUID policyId,
        String code,
        String name,
        String description,
        String conditionType,
        Map<String, Object> conditionParams,
        String actionType,
        Map<String, Object> actionParams,
        int priority,
        String severity,
        boolean stopProcessing,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}