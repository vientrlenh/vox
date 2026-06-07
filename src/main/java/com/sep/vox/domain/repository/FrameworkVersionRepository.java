package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public interface FrameworkVersionRepository {
    Optional<FrameworkVersion> findById(UUID id);
    PageResult<FrameworkVersion> findByFrameworkId(UUID frameworkId, PageRequest pageRequest);
    List<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status);
    Optional<FrameworkVersion> findByFrameworkIdAndVersion(UUID frameworkId, int version);
    FrameworkVersion save(FrameworkVersion version);
    int updateStatus(UUID id, FrameworkVersionStatus status);
    void deleteById(UUID id);
}
