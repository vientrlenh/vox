package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricVersion;

public interface RubricVersionRepository {
    Optional<RubricVersion> findById(UUID id);
    Optional<RubricVersion> findByCode(String code);
    Optional<RubricVersion> findByName(String name);

    List<RubricVersion> findByIdIn(Collection<UUID> ids);
    List<RubricVersion> findByCodeIn(Collection<String> codes);
    List<RubricVersion> findByNameIn(Collection<String> names);

    RubricVersion save(RubricVersion rubricVersion);

    void deleteById(UUID id);

    boolean existsByRubricIdAndIdNot(UUID rubricId, UUID rubricVersionId);

    List<RubricVersion> findByRubricId(UUID rubricId);

    List<RubricVersion> saveAll(List<RubricVersion> rubricVersions);

    void updateRubricVersionAtomic(UUID id, String code, String name, String description, Instant effectiveFrom,
                                   Instant effectiveTo, BigDecimal scoringScaleMin,
                                   BigDecimal scoringScaleMax, String totalScoreMethod,
                                   Instant updatedAt, UUID updatedBy);

    PageResult<RubricVersion> findAllByRubricIdAndStatus(UUID rubricId, String status, int page, int size);

    List<RubricVersion> findByRubricIdInAndStatus(List<UUID> rubricIds, String status);


    PageResult<RubricVersion> searchRubricVersions(UUID rubricId, String keyword, String status, int page, int size);
}
