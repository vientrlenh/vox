package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.rubric.RubricVersionJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.domain.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricVersionMapper;
import com.sep.vox.infrastructure.persistence.audit.AuditStampProvider;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricVersionRepository;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

@Repository
@RequiredArgsConstructor
public class RubricVersionRepositoryImpl implements RubricVersionRepository {

    private final SpringDataRubricVersionRepository springDataRubricVersionRepository;
    private final AuditStampProvider auditStampProvider;

    @Override
    public Optional<RubricVersion> findById(UUID id) {
        return springDataRubricVersionRepository.findById(id).map(RubricVersionMapper::toDomain);
    }

    @Override
    public Optional<RubricVersion> findByCode(String code) {
        return springDataRubricVersionRepository.findByCode(code).map(RubricVersionMapper::toDomain);
    }

    @Override
    public Optional<RubricVersion> findByName(String name) {
        return springDataRubricVersionRepository.findByName(name).map(RubricVersionMapper::toDomain);
    }

    @Override
    public List<RubricVersion> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataRubricVersionRepository.findAllById(ids)
                .stream()
                .map(RubricVersionMapper::toDomain)
                .toList();
    }

    @Override
    public List<RubricVersion> findByCodeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        Set<String> upperCodes = codes.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return springDataRubricVersionRepository.findByCodeIn(upperCodes)
                .stream().map(RubricVersionMapper::toDomain).toList();
    }

    @Override
    public List<RubricVersion> findByNameIn(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return springDataRubricVersionRepository.findByNameIn(names)
                .stream().map(RubricVersionMapper::toDomain).toList();
    }

    @Override
    public RubricVersion save(RubricVersion rubricVersion) {
        RubricVersionJpaEntity entity = RubricVersionMapper.toJpa(rubricVersion);
        RubricVersionJpaEntity saved = springDataRubricVersionRepository.save(entity);
        return RubricVersionMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRubricVersionRepository.deleteById(id);
    }

    @Override
    public boolean existsByRubricIdAndIdNot(UUID rubricId, UUID rubricVersionId) {
        return springDataRubricVersionRepository.existsByRubricIdAndIdNot(rubricId, rubricVersionId);
    }

    @Override
    public List<RubricVersion> findByRubricId(UUID rubricId) {
        return springDataRubricVersionRepository.findByRubricId(rubricId).stream()
                .map(RubricVersionMapper::toDomain)
                .toList();
    }

    @Override
    public List<RubricVersion> saveAll(List<RubricVersion> rubricVersions) {
        List<RubricVersionJpaEntity> entities = rubricVersions.stream()
                .map(RubricVersionMapper::toJpa)
                .toList();
        return springDataRubricVersionRepository.saveAll(entities).stream()
                .map(RubricVersionMapper::toDomain)
                .toList();
    }

    @Override
    public void updateRubricVersionAtomic(
            UUID id, String code, String name, String description, boolean descriptionProvided,
            Instant effectiveFrom, Instant effectiveTo, boolean effectiveToProvided,
            BigDecimal scoringScaleMin, BigDecimal scoringScaleMax, String totalScoreMethod) {
        // SpringDataRubricVersionRepository.updateRubricVersionAtomic declares its parameters in the order (id, name, code, ...)
        DataConflictTranslation.translating(() -> springDataRubricVersionRepository.updateRubricVersionAtomic(
            id, name, code, description, descriptionProvided, effectiveFrom, effectiveTo, effectiveToProvided,
            scoringScaleMin, scoringScaleMax, totalScoreMethod,
            auditStampProvider.currentUpdatedAt(), auditStampProvider.currentUpdatedBy()
        ));
    }

    @Override
    public PageResult<RubricVersion> findAllByRubricIdAndStatus(UUID rubricId, String status, int page, int size) {
        // Optimization: sort by version descending by default (newest version on top)
        Pageable pageable = OneBasedPaging.toPageRequest(page, size, Sort.by("version").descending());

        Page<RubricVersionJpaEntity> entityPage = springDataRubricVersionRepository.findAllByRubricIdAndStatus(rubricId, status, pageable);

        List<RubricVersion> versions = entityPage.getContent().stream()
                .map(RubricVersionMapper::toDomain)
                .toList();

        return OneBasedPaging.toPageResult(entityPage, versions);
    }

    @Override
    public List<RubricVersion> findByRubricIdInAndStatus(List<UUID> rubricIds, String status) {
        return springDataRubricVersionRepository.findByRubricIdInAndStatus(rubricIds, status)
                .stream()
                .map(RubricVersionMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<RubricVersion> searchRubricVersions(UUID rubricId, String keyword, String status, int page, int size) {

        Pageable springPageable = OneBasedPaging.toPageRequest(page, size);

        Page<RubricVersionJpaEntity> pageEntity = springDataRubricVersionRepository.searchRubricVersions(
                rubricId, StringNormalization.toLikePattern(keyword), status, springPageable);

        return OneBasedPaging.toPageResult(
            pageEntity,
            pageEntity.getContent().stream()
                        .map(RubricVersionMapper::toDomain)
                        .toList()
        );
    }
}


