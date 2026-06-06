package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportSession;

public interface ImportSessionRepository {
    ImportSession save(ImportSession session);
    Optional<ImportSession> findById(UUID id);
    Optional<ImportSession> findByIdAndSchoolId(UUID id, UUID schoolId);
}
