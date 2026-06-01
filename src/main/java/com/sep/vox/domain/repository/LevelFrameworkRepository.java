package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.LevelFramework;

public interface LevelFrameworkRepository {
    Optional<LevelFramework> findById(UUID id);
    List<LevelFramework> findAllByIds(Collection<UUID> ids);
}
