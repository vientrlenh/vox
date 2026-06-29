package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.Framework;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;

public interface FrameworkRepository {
    Optional<Framework> findById(UUID id);
    Optional<Framework> findByIdForUpdate(UUID id);
    Optional<Framework> findByCode(String code);
    PageResult<Framework> findAll(int pageNumber, int size, String search, Boolean isActive);
    Framework save(Framework framework);
    void deleteById(UUID id);
}

