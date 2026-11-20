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
                rule.getConditionType(),
                ScoringRuleParamsMapper.toMap(rule.getConditionParams()),
                rule.getActionType(),
                ScoringRuleParamsMapper.toMap(rule.getActionParams()),
                rule.getPriority(),
                rule.getSeverity(),
                rule.isStopProcessing(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}