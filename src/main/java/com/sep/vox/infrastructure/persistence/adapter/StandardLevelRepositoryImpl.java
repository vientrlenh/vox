package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.languagelevel.StandardLevel;
import com.sep.vox.domain.repository.StandardLevelRepository;
import com.sep.vox.infrastructure.persistence.mapper.StandardLevelMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataStandardLevelRepository;


@Repository
public class StandardLevelRepositoryImpl implements StandardLevelRepository {

    private final SpringDataStandardLevelRepository springDataStandardLevelRepository;

    public StandardLevelRepositoryImpl(SpringDataStandardLevelRepository springDataStandardLevelRepository) {
        this.springDataStandardLevelRepository = springDataStandardLevelRepository;
    }

    @Override
    public Optional<StandardLevel> findById(UUID id) {
        return springDataStandardLevelRepository.findById(id)
            .map(StandardLevelMapper::toDomain);
    }

    @Override
    public StandardLevel save(StandardLevel standardLevel) {
        var entity = StandardLevelMapper.toJpa(standardLevel);
        var saved = springDataStandardLevelRepository.save(entity);
        return StandardLevelMapper.toDomain(saved);
    }


}
