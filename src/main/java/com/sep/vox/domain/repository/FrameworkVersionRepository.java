package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public interface FrameworkVersionRepository {
    Optional<FrameworkVersion> findById(UUID id);
    Optional<FrameworkVersion> findByCode(String code);
    Optional<FrameworkVersion> findByName(String name);
    List<FrameworkVersion> findByIdIn(Collection<UUID> ids);
    List<FrameworkVersion> findByCodeIn(Collection<String> codes);
    List<FrameworkVersion> findByNameIn(Collection<String> names);
    Optional<FrameworkVersion> findByIdForUpdate(UUID id);
    PageResult<FrameworkVersion> findByFrameworkId(UUID frameworkId, int pageNumber, int size);
    List<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status);
    PageResult<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status, int pageNumber, int size);
    Optional<FrameworkVersion> findByFrameworkIdAndVersion(UUID frameworkId, int version);
    FrameworkVersion save(FrameworkVersion version);
    int updateStatus(UUID id, FrameworkVersionStatus status);
    void deleteById(UUID id);
    boolean existsByFrameworkId(UUID frameworkId);
}
