package com.sep.vox.domain.repository;

import java.util.Collection;
import com.sep.vox.domain.model.framework.FrameworkResultBand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FrameworkResultBandRepository {
    Optional<FrameworkResultBand> findById(UUID id);

    List<FrameworkResultBand> findAllByIds(List<UUID> ids);
    List<FrameworkResultBand> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkResultBand> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    FrameworkResultBand save(FrameworkResultBand band);
    List<FrameworkResultBand> saveAll(List<FrameworkResultBand> bands);
    void deleteByFrameworkVersionId(UUID frameworkVersionId);
}
