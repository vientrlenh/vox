package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

public interface ImportSessionRepository {
    ImportSession save(ImportSession session);
    Optional<ImportSession> findById(UUID id);
    PageResult<ImportSession> findBySchoolId(UUID schoolId, ImportType type, ImportSessionStatus status, PageRequest pageRequest);
}
