package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricResultBand;

public interface RubricResultBandRepository {
    Optional<RubricResultBand> findById(UUID id);
    RubricResultBand save(RubricResultBand band);

    void deleteById(UUID id);
    void deleteByRubricVersionId(UUID rubricVersionId);
    void saveAll(List<RubricResultBand> bands);
    void updateResultBandAtomic(UUID id, String code, String name, String description, BigDecimal scoreMin, BigDecimal scoreMax, Integer order, java.time.OffsetDateTime updatedAt, UUID updatedBy);
    PageResult<RubricResultBand> findAllByRubricVersionId(UUID rubricVersionId, int page, int size);
}
