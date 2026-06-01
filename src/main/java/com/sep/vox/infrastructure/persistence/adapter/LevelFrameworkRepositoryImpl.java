package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.languagelevel.LevelFramework;
import com.sep.vox.domain.repository.LevelFrameworkRepository;
import com.sep.vox.infrastructure.persistence.mapper.LevelFrameworkMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLevelFrameworkRepository;

@Repository
public class LevelFrameworkRepositoryImpl implements LevelFrameworkRepository {

    private final SpringDataLevelFrameworkRepository springDataLevelFrameworkRepository;

    public LevelFrameworkRepositoryImpl(SpringDataLevelFrameworkRepository springDataLevelFrameworkRepository) {
        this.springDataLevelFrameworkRepository = springDataLevelFrameworkRepository;
    }

    @Override
    public Optional<LevelFramework> findById(UUID id) {
        return springDataLevelFrameworkRepository.findById(id)
            .map(LevelFrameworkMapper::toDomain);
    }

    @Override
    public List<LevelFramework> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataLevelFrameworkRepository.findAllById(ids).stream()
            .map(LevelFrameworkMapper::toDomain)
            .toList();
    }

}
