package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.repository.ScoringRuleRepository;
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
}
