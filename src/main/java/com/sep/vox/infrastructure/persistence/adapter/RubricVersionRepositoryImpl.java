package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricVersionRepository;

@Repository
public class RubricVersionRepositoryImpl implements RubricVersionRepository {

    private final SpringDataRubricVersionRepository springDataRubricVersionRepository;

    public RubricVersionRepositoryImpl(SpringDataRubricVersionRepository springDataRubricVersionRepository) {
        this.springDataRubricVersionRepository = springDataRubricVersionRepository;
    }

    @Override
    public Optional<RubricVersion> findById(UUID id) {
        return springDataRubricVersionRepository.findById(id).map(RubricVersionMapper::toDomain);
    }

    @Override
    public RubricVersion save(RubricVersion rubricVersion) {
        var entity = RubricVersionMapper.toJpa(rubricVersion);
        var saved = springDataRubricVersionRepository.save(entity);
        return RubricVersionMapper.toDomain(saved);
    }
}
