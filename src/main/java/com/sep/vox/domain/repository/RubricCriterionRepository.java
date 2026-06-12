package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricCriterion;

public interface RubricCriterionRepository {
    Optional<RubricCriterion> findById(UUID id);
    RubricCriterion save(RubricCriterion criterion);
    void deleteById(UUID id);
    void deleteByRubricVersionId(UUID rubricVersionId);
    void saveAll(List<RubricCriterion> criteria);
}
