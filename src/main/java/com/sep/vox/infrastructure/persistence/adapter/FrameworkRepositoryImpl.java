package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor 
public class FrameworkRepositoryImpl implements FrameworkRepository {

    private final SpringDataFrameworkRepository springDataFrameworkRepository;

    @Override
    public Optional<Framework> findByIdAndIsActive(UUID id) {
        return springDataFrameworkRepository.findByIdAndStatus(id, FrameworkStatus.ACTIVE.name())
            .map(FrameworkMapper::toDomain);
    }

    @Override
    public Optional<Framework> findById(UUID id) {
        return springDataFrameworkRepository.findById(id)
            .map(FrameworkMapper::toDomain);
    }

    @Override
    public Framework save(Framework framework) {
        FrameworkJpaEntity entity = FrameworkMapper.toJpa(framework);
        FrameworkJpaEntity saved = springDataFrameworkRepository.save(entity);
        return FrameworkMapper.toDomain(saved);
    }

    @Override
    public boolean existsByIdAndIsActive(UUID id) {
        return springDataFrameworkRepository.existsByIdAndStatus(id, FrameworkStatus.ACTIVE.name());
    }

    @Override
    public Optional<Framework> findByIdAndIsActiveForUpdate(UUID id) {
        return springDataFrameworkRepository.findByIdAndStatusForUpdate(id, FrameworkStatus.ACTIVE.name())
            .map(FrameworkMapper::toDomain);
    }

}
