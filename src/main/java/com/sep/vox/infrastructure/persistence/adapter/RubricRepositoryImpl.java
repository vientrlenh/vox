package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricOwnerType;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricRepository;

@Repository
public class RubricRepositoryImpl implements RubricRepository {

    private final SpringDataRubricRepository springDataRubricRepository;

    public RubricRepositoryImpl(SpringDataRubricRepository springDataRubricRepository) {
        this.springDataRubricRepository = springDataRubricRepository;
    }

    @Override
    public Optional<Rubric> findById(UUID id) {
        return springDataRubricRepository.findById(id).map(RubricMapper::toDomain);
    }

    @Override
    public Rubric save(Rubric rubric) {
        var entity = RubricMapper.toJpa(rubric);
        var saved = springDataRubricRepository.save(entity);
        return RubricMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRubricRepository.deleteById(id);
    }

    @Override
    public boolean existsByOwnerTypeAndSchoolIdAndLanguageId(RubricOwnerType ownerType, UUID schoolId, UUID languageId) {
        return springDataRubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageId(ownerType, schoolId, languageId);
    }

    @Override
    public boolean existsByOwnerTypeAndLanguageId(RubricOwnerType ownerType, UUID languageId) {
        return springDataRubricRepository.existsByOwnerTypeAndLanguageId(ownerType, languageId);
    }
}
