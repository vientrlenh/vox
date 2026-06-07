package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkResultBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkResultBandRepository;

@Repository
public class FrameworkResultBandRepositoryImpl implements FrameworkResultBandRepository {

    private final SpringDataFrameworkResultBandRepository springDataFrameworkResultBandRepository;

    public FrameworkResultBandRepositoryImpl(SpringDataFrameworkResultBandRepository springDataFrameworkResultBandRepository) {
        this.springDataFrameworkResultBandRepository = springDataFrameworkResultBandRepository;
    }

    @Override
    public List<FrameworkResultBand> findByFrameworkVersionId(UUID frameworkVersionId) {
        return springDataFrameworkResultBandRepository.findByFrameworkVersionId(frameworkVersionId)
            .stream().map(FrameworkResultBandMapper::toDomain).toList();
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
