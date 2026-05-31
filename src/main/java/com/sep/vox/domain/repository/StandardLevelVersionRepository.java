package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.StandardLevelVersion;

public interface StandardLevelVersionRepository {
    Optional<StandardLevelVersion> findById(UUID id);
    StandardLevelVersion save(StandardLevelVersion version);
}
