package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.StandardLevel;


public interface StandardLevelRepository {
    Optional<StandardLevel> findById(UUID id);
    List<StandardLevel> findAllByIds(Collection<UUID> ids);
    StandardLevel save(StandardLevel standardLevel);
}
