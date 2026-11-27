package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.scoringrule.ScoringRule;

public interface ScoringRuleRepository {
    Optional<ScoringRule> findById(UUID id);
    ScoringRule save(ScoringRule rule);

    // Checks for a duplicate Code within 1 Assessment Policy (code only needs to be unique per Policy)
    boolean existsByPolicyIdAndCode(UUID policyId, String code);

    // Search/paginate the Scoring Rules of 1 Assessment Policy, sorted by priority ascending
    PageResult<ScoringRule> searchByPolicyId(UUID policyId, String keyword, Boolean isActive, int page, int size);

    void deleteById(UUID id);
}
