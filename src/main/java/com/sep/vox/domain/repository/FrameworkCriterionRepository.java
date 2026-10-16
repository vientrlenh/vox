package com.sep.vox.domain.repository;

import com.sep.vox.domain.model.framework.FrameworkCriterion;

import java.util.Collection;
import java.util.List;

public interface FrameworkCriterionRepository {
    List<FrameworkCriterion> saveAll(Collection<FrameworkCriterion> criteria);
    
}
