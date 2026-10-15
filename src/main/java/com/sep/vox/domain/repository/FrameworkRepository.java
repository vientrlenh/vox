package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.framework.Framework;

public interface FrameworkRepository {
    Optional<Framework> findByIdAndIsActive(UUID id);
    Optional<Framework> findByIdAndIsActiveForUpdate(UUID id);
    boolean existsByIdAndIsActive(UUID id);
    Optional<Framework> findById(UUID id);
    Framework save(Framework framework);
}

