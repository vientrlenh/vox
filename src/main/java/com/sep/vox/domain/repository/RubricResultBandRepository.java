package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricResultBand;

public interface RubricResultBandRepository {
    Optional<RubricResultBand> findById(UUID id);
    RubricResultBand save(RubricResultBand band);

    void deleteById(UUID id);
    void deleteByRubricVersionId(UUID rubricVersionId);
    void saveAll(List<RubricResultBand> bands);
}
