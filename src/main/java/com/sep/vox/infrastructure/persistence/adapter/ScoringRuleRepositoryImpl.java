package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.infrastructure.persistence.mapper.ScoringRuleMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataScoringRuleRepository;

@Repository
public class ScoringRuleRepositoryImpl implements ScoringRuleRepository {

    private final SpringDataScoringRuleRepository springDataScoringRuleRepository;

    public ScoringRuleRepositoryImpl(SpringDataScoringRuleRepository springDataScoringRuleRepository) {
        this.springDataScoringRuleRepository = springDataScoringRuleRepository;
    }

    @Override
    public Optional<ScoringRule> findById(UUID id) {
        return springDataScoringRuleRepository.findById(id).map(ScoringRuleMapper::toDomain);
    }

    @Override
    public ScoringRule save(ScoringRule rule) {
        var entity = ScoringRuleMapper.toJpa(rule);
        var saved = springDataScoringRuleRepository.save(entity);
        return ScoringRuleMapper.toDomain(saved);
    }

    @Override
    public boolean existsByPolicyIdAndCode(UUID policyId, String code) {
        return springDataScoringRuleRepository.existsByPolicyIdAndCode(policyId, code);
    }

    @Override
    public PageResult<ScoringRule> searchByPolicyId(UUID policyId, String keyword, Boolean isActive, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var pageEntity = springDataScoringRuleRepository.searchByPolicyId(
                policyId, StringNormalization.toLikePattern(keyword), isActive, pageable);

        return PageResult.fromRepository(
                pageEntity.getContent().stream().map(ScoringRuleMapper::toDomain).toList(),
                page,
                size,
                pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    @Override
    public void deleteById(UUID id) {
        springDataScoringRuleRepository.deleteById(id);
    }
}
