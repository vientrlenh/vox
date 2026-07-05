package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    RubricVersion save(RubricVersion rubricVersion);

    void deleteById(UUID id);

    boolean existsByRubricIdAndIdNot(UUID rubricId, UUID rubricVersionId);

    List<RubricVersion> findByRubricId(UUID rubricId);

    void saveAll(List<RubricVersion> rubricVersions);

    void updateRubricVersionAtomic(UUID id, String code, String name, String description, OffsetDateTime effectiveFrom,
                                   OffsetDateTime effectiveTo, BigDecimal scoringScaleMin,
                                   BigDecimal scoringScaleMax, String totalScoreMethod,
                                   OffsetDateTime updatedAt, UUID updatedBy);

    PageResult<RubricVersion> findAllByRubricIdAndStatus(UUID rubricId, String status, int page, int size);

    List<RubricVersion> findByRubricIdInAndStatus(List<UUID> rubricIds, String status);


    PageResult<RubricVersion> searchRubricVersions(UUID rubricId, String keyword, String status, int page, int size);

    int archiveExpiredPublishedVersions(OffsetDateTime now);
}
