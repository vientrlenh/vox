package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricVersionRepository;

@Repository
public class RubricVersionRepositoryImpl implements RubricVersionRepository {

    private final SpringDataRubricVersionRepository springDataRubricVersionRepository;

    public RubricVersionRepositoryImpl(SpringDataRubricVersionRepository springDataRubricVersionRepository) {
        this.springDataRubricVersionRepository = springDataRubricVersionRepository;
    }

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
            UUID id,
            String code,
            String name,
            String description,
            Instant effectiveFrom,
            Instant effectiveTo,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax,
            String totalScoreMethod,
            Instant updatedAt,
            UUID updatedBy) {
        // SpringDataRubricVersionRepository.updateRubricVersionAtomic khai báo tham số theo thứ tự (id, name, code, ...)
        springDataRubricVersionRepository.updateRubricVersionAtomic(
            id,
            name,
            code,
            description,
            effectiveFrom,
            effectiveTo,
            scoringScaleMin,
            scoringScaleMax,
            totalScoreMethod,
            updatedAt,
            updatedBy
        );
    }

    @Override
    public PageResult<RubricVersion> findAllByRubricIdAndStatus(UUID rubricId, String status, int page, int size) {
        // Optimization: sort by version descending by default (newest version on top)
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("version").descending());

        Page<RubricVersionJpaEntity> entityPage = springDataRubricVersionRepository.findAllByRubricIdAndStatus(rubricId, status, pageable);

        List<RubricVersion> versions = entityPage.getContent().stream()
                .map(RubricVersionMapper::toDomain)
                .toList();

        return new PageResult<>(
                versions,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
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

        Pageable springPageable = PageRequest.of(page - 1, size);

        Page<RubricVersionJpaEntity> pageEntity = springDataRubricVersionRepository.searchRubricVersions(
                rubricId, StringNormalization.toLikePattern(keyword), status, springPageable);

        return new PageResult<>(
                pageEntity.getContent().stream()
                        .map(RubricVersionMapper::toDomain)
                        .toList(),
                pageEntity.getNumber(),
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }
}


