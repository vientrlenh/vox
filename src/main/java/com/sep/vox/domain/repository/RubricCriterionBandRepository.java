package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricCriterionBand;

public interface RubricCriterionBandRepository {
    Optional<RubricCriterionBand> findById(UUID id);
    RubricCriterionBand save(RubricCriterionBand band);
    void deleteByCriterionId(UUID criterionId);
    void deleteByRubricVersionId(UUID rubricVersionId);
    void saveAll(List<RubricCriterionBand> rubricCriterionBands);
    void deleteById(UUID id);
    void updateBandAtomic(UUID id, String code, BigDecimal scoreMin, BigDecimal scoreMax, OffsetDateTime updatedAt, UUID updatedBy);
    PageResult<RubricCriterionBand> findAllByCriterionId(UUID criterionId, int page, int size);
    List<RubricCriterionBand> findByCriterionIdIn(List<UUID> criterionIds);
}
