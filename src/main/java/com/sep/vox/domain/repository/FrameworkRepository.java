package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.RubricVersion;

import java.util.Optional;
import java.util.UUID;

public interface FrameworkRepository {
    Optional<Framework> findById(UUID id);
}

