package com.sep.vox.domain.repository;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricResultBand;

public interface RubricResultBandRepository {
    Optional<RubricResultBand> findById(UUID id);
    List<RubricResultBand> findByIdIn(Collection<UUID> ids);
    RubricResultBand save(RubricResultBand band);

    void deleteById(UUID id);
    void deleteByRubricVersionId(UUID rubricVersionId);
    List<RubricResultBand> saveAll(List<RubricResultBand> bands);
    void updateResultBandAtomic(UUID id, String code, String name, String description, BigDecimal scoreMin, BigDecimal scoreMax, Integer order, java.time.Instant updatedAt, UUID updatedBy);
    PageResult<RubricResultBand> findAllByRubricVersionId(UUID rubricVersionId, int page, int size);
    List<RubricResultBand> findByRubricVersionIdIn(List<UUID> versionIds);
    PageResult<RubricResultBand> searchRubricResultBands(UUID versionId, String keyword, int page, int size);
    List<RubricResultBand> findByRubricVersionId(UUID rubricVersionId);
}
