package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkResultBand;

public interface FrameworkResultBandRepository {
    List<FrameworkResultBand> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkResultBand> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    FrameworkResultBand save(FrameworkResultBand band);
    List<FrameworkResultBand> saveAll(List<FrameworkResultBand> bands);
    void deleteByFrameworkVersionId(UUID frameworkVersionId);
}
