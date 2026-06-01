package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.languagelevel.SchoolLevelVersion;

public interface SchoolLevelVersionRepository {
    Optional<SchoolLevelVersion> findById(UUID id);
    SchoolLevelVersion save(SchoolLevelVersion version);
}
