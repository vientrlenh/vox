package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricCriterionRepository;

@Repository
public class RubricCriterionRepositoryImpl implements RubricCriterionRepository {

    private final SpringDataRubricCriterionRepository springDataRubricCriterionRepository;

    public RubricCriterionRepositoryImpl(SpringDataRubricCriterionRepository springDataRubricCriterionRepository) {
        this.springDataRubricCriterionRepository = springDataRubricCriterionRepository;
    }

    @Override
    public Optional<RubricCriterion> findById(UUID id) {
        return springDataRubricCriterionRepository.findById(id).map(RubricCriterionMapper::toDomain);
    }

    @Override
    public RubricCriterion save(RubricCriterion criterion) {
        var entity = RubricCriterionMapper.toJpa(criterion);
        var saved = springDataRubricCriterionRepository.save(entity);
        return RubricCriterionMapper.toDomain(saved);
    }
}
