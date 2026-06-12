package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.FrameworkResultBand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface FrameworkResultBandRepository {
    Optional<FrameworkResultBand> findById(UUID id);

    List<FrameworkResultBand> findAllByIds(List<UUID> ids);
}
