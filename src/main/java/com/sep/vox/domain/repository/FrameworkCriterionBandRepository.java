package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;

public interface FrameworkCriterionBandRepository {
    List<FrameworkCriterionBand> findByFrameworkCriterionId(UUID frameworkCriterionId);
    List<FrameworkCriterionBand> findByFrameworkCriterionIdIn(Collection<UUID> frameworkCriterionIds);
    List<FrameworkCriterionBand> findByFrameworkResultBandId(UUID frameworkResultBandId);
    FrameworkCriterionBand save(FrameworkCriterionBand band);
    List<FrameworkCriterionBand> saveAll(List<FrameworkCriterionBand> bands);
    void deleteByFrameworkCriterionId(UUID frameworkCriterionId);
}
