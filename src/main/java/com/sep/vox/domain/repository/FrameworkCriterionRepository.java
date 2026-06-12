package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.FrameworkCriterion;

import java.util.List;
import java.util.UUID;

public interface FrameworkCriterionRepository {
    List<FrameworkCriterion> findAllByIds(List<UUID> ids);}
