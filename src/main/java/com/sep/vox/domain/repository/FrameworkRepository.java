package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.Framework;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;

public interface FrameworkRepository {
    Optional<Framework> findFrameworkById(UUID id);
    List<Framework> findByIdIn(Collection<UUID> ids);
    PageResult<Framework> findAllActive(int pageNumber, int size);
    Optional<Framework> findFrameworkByIdForUpdate(UUID id);
    Optional<Framework> findFrameworkByCode(String code);
    PageResult<Framework> findAllFrameworks(int pageNumber, int size, String search, Boolean isActive);
    Framework saveFramework(Framework framework);
    void deleteFrameworkById(UUID id);
}

