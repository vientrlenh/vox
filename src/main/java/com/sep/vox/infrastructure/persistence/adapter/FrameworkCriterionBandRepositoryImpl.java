package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
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
    public List<FrameworkCriterionBand> findByFrameworkCriterionId(UUID frameworkCriterionId) {
        return springDataFrameworkCriterionBandRepository.findByFrameworkCriterionId(frameworkCriterionId)
            .stream().map(FrameworkCriterionBandMapper::toDomain).toList();
    }

    @Override
    public List<FrameworkCriterionBand> findByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds) {
        return springDataFrameworkCriterionBandRepository.findByFrameworkCriterionIdIn(frameworkCriterionIds)
            .stream().map(FrameworkCriterionBandMapper::toDomain).toList();
    }

    @Override
    public List<FrameworkCriterionBand> findByFrameworkResultBandId(UUID frameworkResultBandId) {
        return springDataFrameworkCriterionBandRepository.findByFrameworkResultBandId(frameworkResultBandId)
            .stream().map(FrameworkCriterionBandMapper::toDomain).toList();
    }

    @Override
    public FrameworkCriterionBand save(FrameworkCriterionBand band) {
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
    public void deleteByFrameworkCriterionId(UUID frameworkCriterionId) {
        springDataFrameworkCriterionBandRepository.deleteByFrameworkCriterionId(frameworkCriterionId);
    }
}
