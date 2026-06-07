package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.Framework;

public interface FrameworkRepository {
    Optional<Framework> findById(UUID id);
    Optional<Framework> findByCode(String code);
    PageResult<Framework> findAll(PageRequest pageRequest);
    Framework save(Framework framework);
    int updateCurrentVersionId(UUID id, UUID currentVersionId);
}
