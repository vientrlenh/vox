package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.RubricCriterionBand;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricCriterionBandRepository;

@Repository
public class RubricCriterionBandRepositoryImpl implements RubricCriterionBandRepository {

    private final SpringDataRubricCriterionBandRepository springDataRubricCriterionBandRepository;

    public RubricCriterionBandRepositoryImpl(SpringDataRubricCriterionBandRepository springDataRubricCriterionBandRepository) {
        this.springDataRubricCriterionBandRepository = springDataRubricCriterionBandRepository;
    }

    @Override
    public Optional<RubricCriterionBand> findById(UUID id) {
        return springDataRubricCriterionBandRepository.findById(id).map(RubricCriterionBandMapper::toDomain);
    }

    @Override
    public RubricCriterionBand save(RubricCriterionBand band) {
        var entity = RubricCriterionBandMapper.toJpa(band);
        var saved = springDataRubricCriterionBandRepository.save(entity);
        return RubricCriterionBandMapper.toDomain(saved);
    }
}
