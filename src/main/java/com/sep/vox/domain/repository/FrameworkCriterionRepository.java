package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkCriterion;

public interface FrameworkCriterionRepository {
    List<FrameworkCriterion> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkCriterion> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    FrameworkCriterion save(FrameworkCriterion criterion);
    List<FrameworkCriterion> saveAll(List<FrameworkCriterion> criteria);
    void deleteByFrameworkVersionId(UUID frameworkVersionId);
}
