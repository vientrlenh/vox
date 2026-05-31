package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubriclevelmapping.RubricLevelMapping;
import com.sep.vox.domain.repository.RubricLevelMappingRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricLevelMappingMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricLevelMappingRepository;

@Repository
public class RubricLevelMappingRepositoryImpl implements RubricLevelMappingRepository {

    private final SpringDataRubricLevelMappingRepository springDataRubricLevelMappingRepository;

    public RubricLevelMappingRepositoryImpl(SpringDataRubricLevelMappingRepository springDataRubricLevelMappingRepository) {
        this.springDataRubricLevelMappingRepository = springDataRubricLevelMappingRepository;
    }

    @Override
    public Optional<RubricLevelMapping> findById(UUID id) {
        return springDataRubricLevelMappingRepository.findById(id).map(RubricLevelMappingMapper::toDomain);
    }

    @Override
    public RubricLevelMapping save(RubricLevelMapping mapping) {
        var entity = RubricLevelMappingMapper.toJpa(mapping);
        var saved = springDataRubricLevelMappingRepository.save(entity);
        return RubricLevelMappingMapper.toDomain(saved);
    }
}
