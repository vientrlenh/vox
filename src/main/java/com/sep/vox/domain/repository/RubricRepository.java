package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.Rubric;

public interface RubricRepository {
    Optional<Rubric> findById(UUID id);
    Rubric save(Rubric rubric);
}
