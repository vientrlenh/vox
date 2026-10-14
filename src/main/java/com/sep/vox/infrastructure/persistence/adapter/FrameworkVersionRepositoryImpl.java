package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.infrastructure.persistence.entity.FrameworkVersionJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkVersionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor 
public class FrameworkVersionRepositoryImpl implements FrameworkVersionRepository {

    private final SpringDataFrameworkVersionRepository springDataFrameworkVersionRepository;

    @Override
    public Optional<FrameworkVersion> findById(UUID id) {
        return springDataFrameworkVersionRepository.findById(id).map(FrameworkVersionMapper::toDomain);
    }

    @Override
    public boolean containsDraftInFrameworkId(UUID frameworkId) {
        return springDataFrameworkVersionRepository.existsByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.DRAFT.name());
    }

    @Override
    public long countPublishedAndArchivedOnFrameworkId(UUID frameworkId) {
        return springDataFrameworkVersionRepository.countPublishedAndArchivedOnFrameworkId(frameworkId);
    }

    @Override
    public FrameworkVersion save(FrameworkVersion version) {
        FrameworkVersionJpaEntity entity = FrameworkVersionMapper.toJpa(version);
        FrameworkVersionJpaEntity saved = springDataFrameworkVersionRepository.save(entity);
        return FrameworkVersionMapper.toDomain(saved);
    }

    @Override
    public FrameworkVersion saveAndFlush(FrameworkVersion version) {
        FrameworkVersionJpaEntity entity = FrameworkVersionMapper.toJpa(version);
        FrameworkVersionJpaEntity saved = springDataFrameworkVersionRepository.saveAndFlush(entity);
        return FrameworkVersionMapper.toDomain(saved);
    }

    
    
}
