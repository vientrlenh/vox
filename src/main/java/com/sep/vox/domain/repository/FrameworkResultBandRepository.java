package com.sep.vox.domain.repository;

import java.util.Collection;
import com.sep.vox.domain.model.framework.FrameworkResultBand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FrameworkResultBandRepository {
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);
    boolean existsByFrameworkVersionIdAndCodeAndIdNot(UUID frameworkVersionId, String code, UUID id);
    boolean existsByFrameworkVersionIdAndLabelAndIdNot(UUID frameworkVersionId, String label, UUID id);
    boolean existsByFrameworkVersionIdAndCodeIn(UUID frameworkVersionId, Collection<String> codes);
    boolean existsByFrameworkVersionIdAndLabelIn(UUID frameworkVersionId, Collection<String> labels);
    Optional<FrameworkResultBand> findById(UUID id);
    Optional<FrameworkResultBand> findByVersionIdAndCode(UUID frameworkVersionId, String code);
    Optional<FrameworkResultBand> findByVersionIdAndName(UUID frameworkVersionId, String name);

    List<FrameworkResultBand> findAllByIds(List<UUID> ids);
    List<FrameworkResultBand> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkResultBand> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    List<FrameworkResultBand> findByFrameworkVersionIdAndCodeIn(UUID frameworkVersionId, Collection<String> codes);
    FrameworkResultBand save(FrameworkResultBand band);
    List<FrameworkResultBand> saveAll(List<FrameworkResultBand> bands);
    void deleteByFrameworkVersionId(UUID frameworkVersionId);
    void deleteById(UUID id);
}
