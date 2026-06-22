package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricCriterion;

public interface RubricCriterionRepository {
    Optional<RubricCriterion> findById(UUID id);
    RubricCriterion save(RubricCriterion criterion);
    void deleteById(UUID id);
    void deleteByRubricVersionId(UUID rubricVersionId);
    void saveAll(List<RubricCriterion> criteria);

    void updateCriterionAtomic(UUID id, String code, String name, String description, String examplesJson, BigDecimal weight, BigDecimal minScore, BigDecimal maxScore, Integer order, Boolean isRequired, OffsetDateTime updatedAt, UUID updatedBy);
    PageResult<RubricCriterion> findAllByRubricVersionId(UUID rubricVersionId, int page, int size);

    List<RubricCriterion> findByRubricVersionIdIn(List<UUID> versionIds);
}
