package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricCriterion;

public interface RubricCriterionRepository {
    Optional<RubricCriterion> findById(UUID id);
    RubricCriterion save(RubricCriterion criterion);
}
