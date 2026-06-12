package com.sep.vox.domain.repository;


import com.sep.vox.domain.model.rubric.RubricApplicability;

import java.util.List;

public interface RubricApplicabilityRepository {
    void saveAll(List<RubricApplicability> rubricApplicabilities);
}
