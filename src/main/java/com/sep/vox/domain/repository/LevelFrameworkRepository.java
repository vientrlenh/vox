package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.LevelFramework;

public interface LevelFrameworkRepository {
    Optional<LevelFramework> findById(UUID id);
}
