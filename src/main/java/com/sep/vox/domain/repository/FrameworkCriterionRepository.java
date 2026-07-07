package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.FrameworkCriterion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FrameworkCriterionRepository {
    Optional<FrameworkCriterion> findById(UUID id);
    List<FrameworkCriterion> findAllByIds(List<UUID> ids);
    List<FrameworkCriterion> findByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkCriterion> findByFrameworkVersionIdIn(Collection<UUID> frameworkVersionIds);
    FrameworkCriterion save(FrameworkCriterion criterion);
    List<FrameworkCriterion> saveAll(List<FrameworkCriterion> criteria);
    void deleteByFrameworkVersionId(UUID frameworkVersionId);
    List<FrameworkCriterion> findByFrameworkId(UUID frameworkId);
}
