package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkCriterionBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkCriterionBandRepository;

@Repository
public class FrameworkCriterionBandRepositoryImpl implements FrameworkCriterionBandRepository {

    private final SpringDataFrameworkCriterionBandRepository springDataFrameworkCriterionBandRepository;

    public FrameworkCriterionBandRepositoryImpl(SpringDataFrameworkCriterionBandRepository springDataFrameworkCriterionBandRepository) {
        this.springDataFrameworkCriterionBandRepository = springDataFrameworkCriterionBandRepository;
    }

    @Override
    public Optional<FrameworkCriterionBand> findById(UUID id) {
        return springDataFrameworkCriterionBandRepository.findById(id).map(FrameworkCriterionBandMapper::toDomain);
    }

    @Override
    public List<FrameworkCriterionBand> findByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds) {
        return springDataFrameworkCriterionBandRepository.findByFrameworkCriterionIdIn(frameworkCriterionIds)
            .stream().map(FrameworkCriterionBandMapper::toDomain).toList();
    }

    @Override
    public FrameworkCriterionBand saveCriterionBand(FrameworkCriterionBand band) {
        var entity = FrameworkCriterionBandMapper.toJpa(band);
        var saved = springDataFrameworkCriterionBandRepository.save(entity);
        return FrameworkCriterionBandMapper.toDomain(saved);
    }

    @Override
    public List<FrameworkCriterionBand> saveAll(List<FrameworkCriterionBand> bands) {
        var entities = bands.stream().map(FrameworkCriterionBandMapper::toJpa).toList();
        return springDataFrameworkCriterionBandRepository.saveAll(entities)
            .stream().map(FrameworkCriterionBandMapper::toDomain).toList();
    }

    @Override
    public void deleteByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds) {
        springDataFrameworkCriterionBandRepository.deleteByFrameworkCriterionIdIn(frameworkCriterionIds);
    }

    @Override
    public void deleteByFrameworkVersionId(UUID frameworkVersionId) {
        springDataFrameworkCriterionBandRepository.deleteByFrameworkVersionId(frameworkVersionId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataFrameworkCriterionBandRepository.deleteById(id);
    }

    @Override
    public boolean existsByFrameworkResultBandId(UUID frameworkResultBandId) {
        return springDataFrameworkCriterionBandRepository.existsByFrameworkResultBandId(frameworkResultBandId);
    }
}
