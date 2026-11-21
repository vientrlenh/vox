package com.sep.vox.application.mapper.scoringrule;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.model.scoringrule.ScoringRule;

@Service
public class ScoringRuleDtoMapper {

    private final ScoringRuleParamsMapper scoringRuleParamsMapper;

    public ScoringRuleDtoMapper(ScoringRuleParamsMapper scoringRuleParamsMapper) {
        this.scoringRuleParamsMapper = scoringRuleParamsMapper;
    }

    public ScoringRuleDto toDto(ScoringRule rule) {
        return new ScoringRuleDto(
                rule.getId(),
                rule.getPolicyId(),
                rule.getCode(),
                rule.getName(),
                rule.getDescription(),
                rule.getConditionType(),
                scoringRuleParamsMapper.toMap(rule.getConditionParams()),
                rule.getActionType(),
                scoringRuleParamsMapper.toMap(rule.getActionParams()),
                rule.getPriority(),
                rule.getSeverity(),
                rule.isStopProcessing(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
