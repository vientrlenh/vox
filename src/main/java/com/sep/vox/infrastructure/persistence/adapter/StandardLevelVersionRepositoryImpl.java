package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.languagelevel.StandardLevelVersion;
import com.sep.vox.domain.repository.StandardLevelVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.StandardLevelVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataStandardLevelVersionRepository;

@Repository
public class StandardLevelVersionRepositoryImpl implements StandardLevelVersionRepository {

    private final SpringDataStandardLevelVersionRepository springDataStandardLevelVersionRepository;

    public StandardLevelVersionRepositoryImpl(SpringDataStandardLevelVersionRepository springDataStandardLevelVersionRepository) {
        this.springDataStandardLevelVersionRepository = springDataStandardLevelVersionRepository;
    }

    @Override
    public Optional<StandardLevelVersion> findById(UUID id) {
        return springDataStandardLevelVersionRepository.findById(id)
            .map(StandardLevelVersionMapper::toDomain);
    }

    @Override
    public StandardLevelVersion save(StandardLevelVersion version) {
        var entity = StandardLevelVersionMapper.toJpa(version);
        var saved = springDataStandardLevelVersionRepository.save(entity);
        return StandardLevelVersionMapper.toDomain(saved);
    }
    
}
