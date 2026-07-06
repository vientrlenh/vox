package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public interface FrameworkVersionRepository {
    Optional<FrameworkVersion> findFrameworkVersionById(UUID id);
    Optional<FrameworkVersion> findFrameworkVersionByIdForUpdate(UUID id);
    PageResult<FrameworkVersion> findByFrameworkId(UUID frameworkId, int pageNumber, int size);
    List<FrameworkVersion> findByFrameworkVersionIdAndStatus(UUID frameworkId, FrameworkVersionStatus status);
    Optional<FrameworkVersion> findByFrameworkIdAndVersion(UUID frameworkId, int version);
    FrameworkVersion saveFrameworkVersion(FrameworkVersion version);
    int updateFrameworkVersionStatus(UUID id, FrameworkVersionStatus status);
    void deleteFrameworkVersionById(UUID id);
    boolean existsByFrameworkId(UUID frameworkId);
}
