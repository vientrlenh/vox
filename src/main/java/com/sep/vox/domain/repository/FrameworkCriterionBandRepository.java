package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;

public interface FrameworkCriterionBandRepository {
    Optional<FrameworkCriterionBand> findById(UUID id);
    List<FrameworkCriterionBand> findByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds);
    FrameworkCriterionBand save(FrameworkCriterionBand band);
    List<FrameworkCriterionBand> saveAll(List<FrameworkCriterionBand> bands);
    void deleteByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds);
    void deleteByFrameworkVersionId(UUID frameworkVersionId);
    void deleteById(UUID id);
    boolean existsByFrameworkResultBandId(UUID frameworkResultBandId);
}
