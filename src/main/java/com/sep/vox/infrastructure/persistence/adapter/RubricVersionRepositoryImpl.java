package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
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

    @Override
    public void deleteById(UUID id) {
        springDataRubricVersionRepository.deleteById(id);
    }

    @Override
    public boolean existsByRubricIdAndIdNot(UUID rubricId, UUID rubricVersionId) {
       return springDataRubricVersionRepository.existsByRubricIdAndIdNot(rubricId, rubricVersionId);
    }

    @Override
    public List<RubricVersion> findByRubricId(UUID rubricId) {
        return springDataRubricVersionRepository.findByRubricId(rubricId).stream()
                .map(RubricVersionMapper::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<RubricVersion> rubricVersions) {
        var entities = rubricVersions.stream()
                .map(RubricVersionMapper::toJpa)
                .toList();
        springDataRubricVersionRepository.saveAll(entities);
    }
}


