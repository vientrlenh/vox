package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricCriterionBand;

public interface RubricCriterionBandRepository {
    Optional<RubricCriterionBand> findById(UUID id);
    RubricCriterionBand save(RubricCriterionBand band);
}
