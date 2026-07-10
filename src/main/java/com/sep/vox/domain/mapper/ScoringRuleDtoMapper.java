package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.model.scoringrule.ScoringRule;

public final class ScoringRuleDtoMapper {

    private ScoringRuleDtoMapper() {
    }

    public static ScoringRuleDto toDto(ScoringRule rule) {
        return new ScoringRuleDto(
                rule.getId(),
                rule.getPolicyId(),
                rule.getCode(),
                rule.getName(),
                rule.getDescription(),
                rule.getConditionType() != null ? rule.getConditionType().name() : null,
                ScoringRuleParamsMapper.toMap(rule.getConditionParams()),
                rule.getActionType() != null ? rule.getActionType().name() : null,
                ScoringRuleParamsMapper.toMap(rule.getActionParams()),
                rule.getPriority(),
                rule.getSeverity() != null ? rule.getSeverity().name() : null,
                rule.isStopProcessing(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}