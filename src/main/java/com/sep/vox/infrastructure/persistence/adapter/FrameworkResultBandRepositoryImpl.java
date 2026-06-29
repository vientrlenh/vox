package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkResultBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkResultBandRepository;

import java.util.Optional;


@Repository
public class FrameworkResultBandRepositoryImpl implements FrameworkResultBandRepository {

    private final SpringDataFrameworkResultBandRepository springDataFrameworkResultBandRepository;

    public FrameworkResultBandRepositoryImpl(SpringDataFrameworkResultBandRepository springDataFrameworkResultBandRepository) {
        this.springDataFrameworkResultBandRepository = springDataFrameworkResultBandRepository;
    }

    @Override
    public boolean existsByFrameworkVersionId(UUID frameworkVersionId) {
        return springDataFrameworkResultBandRepository.existsByFrameworkVersionId(frameworkVersionId);
    }

    @Override
    public List<FrameworkResultBand> findByFrameworkVersionId(UUID frameworkVersionId) {
        return springDataFrameworkResultBandRepository.findByFrameworkVersionId(frameworkVersionId)
                .stream().map(FrameworkResultBandMapper::toDomain).toList();
    }

    @Override
    public Optional<FrameworkResultBand> findById(UUID id) {
        return springDataFrameworkResultBandRepository.findById(id)
                .map(FrameworkResultBandMapper::toDomain);
    }

    @Override
    public List<FrameworkResultBand> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds) {
        return springDataFrameworkResultBandRepository.findByFrameworkVersionIdIn(frameworkVersionIds)
                .stream().map(FrameworkResultBandMapper::toDomain).toList();
    }


    @Override
    public List<FrameworkResultBand> findAllByIds(List<UUID> ids) {
        return springDataFrameworkResultBandRepository.findAllById(ids)
                .stream()
                .map(FrameworkResultBandMapper::toDomain)
                .toList();
    }

    @Override
    public FrameworkResultBand save(FrameworkResultBand band) {
        var entity = FrameworkResultBandMapper.toJpa(band);
        var saved = springDataFrameworkResultBandRepository.save(entity);
        return FrameworkResultBandMapper.toDomain(saved);
    }

    @Override
    public List<FrameworkResultBand> saveAll(List<FrameworkResultBand> bands) {
        var entities = bands.stream().map(FrameworkResultBandMapper::toJpa).toList();
        return springDataFrameworkResultBandRepository.saveAll(entities)
                .stream().map(FrameworkResultBandMapper::toDomain).toList();
    }

    @Override
    public void deleteByFrameworkVersionId(UUID frameworkVersionId) {
        springDataFrameworkResultBandRepository.deleteByFrameworkVersionId(frameworkVersionId);
    }
}
