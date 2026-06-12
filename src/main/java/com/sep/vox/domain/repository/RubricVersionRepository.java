package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricVersion;

public interface RubricVersionRepository {
    Optional<RubricVersion> findById(UUID id);
    RubricVersion save(RubricVersion rubricVersion);
    void deleteById(UUID id);
    boolean existsByRubricIdAndIdNot(UUID rubricId, UUID rubricVersionId);

    List<RubricVersion> findByRubricId(UUID rubricId);
    void saveAll(List<RubricVersion> rubricVersions);
}
