package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRule;

public interface ScoringRuleRepository {
    Optional<ScoringRule> findById(UUID id);
    ScoringRule save(ScoringRule rule);
}
