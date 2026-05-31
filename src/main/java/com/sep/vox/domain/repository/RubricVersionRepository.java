package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricVersion;

public interface RubricVersionRepository {
    Optional<RubricVersion> findById(UUID id);
    RubricVersion save(RubricVersion rubricVersion);
}
