package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.Framework;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;

public interface FrameworkRepository {
    List<Framework> findByIdIn(Collection<UUID> ids);
    Optional<Framework> findFrameworkByIdForUpdate(UUID id);
    Optional<Framework> findById(UUID id);
    Optional<Framework> findByCode(String code);
    PageResult<Framework> findAll(int pageNumber, int size, String search, Boolean isActive);
    PageResult<Framework> findAllActive(int pageNumber, int size);
    Framework save(Framework framework);
    void deleteById(UUID id);
}

