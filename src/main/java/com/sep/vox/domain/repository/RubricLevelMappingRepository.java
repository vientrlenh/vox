package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubriclevelmapping.RubricLevelMapping;

public interface RubricLevelMappingRepository {
    Optional<RubricLevelMapping> findById(UUID id);
    RubricLevelMapping save(RubricLevelMapping mapping);
}
