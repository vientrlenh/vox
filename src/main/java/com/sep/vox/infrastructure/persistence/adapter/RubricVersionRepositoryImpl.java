package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
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
    public RubricVersion save(RubricVersion rubricVersion) {
        var entity = RubricVersionMapper.toJpa(rubricVersion);
        var saved = springDataRubricVersionRepository.save(entity);
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
    public void saveAll(List<RubricVersion> rubricVersions) {
        var entities = rubricVersions.stream()
                .map(RubricVersionMapper::toJpa)
                .toList();
        springDataRubricVersionRepository.saveAll(entities);
    }

    @Override
    public void updateRubricVersionAtomic(UUID id, String code, String name, String description, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo, BigDecimal scoringScaleMin, BigDecimal scoringScaleMax, String totalScoreMethod, OffsetDateTime updatedAt, UUID updatedBy) {
        springDataRubricVersionRepository.updateRubricVersionAtomic(id, code, name, description, effectiveFrom, effectiveTo, scoringScaleMin, scoringScaleMax, totalScoreMethod, updatedAt, updatedBy);
    }

    @Override
    public PageResult<RubricVersion> findAllByRubricIdAndStatus(UUID rubricId, String status, int page, int size) {
        // Tối ưu: Mặc định sort theo version giảm dần (bản mới nhất nằm trên cùng)
        Pageable pageable = PageRequest.of(page, size, Sort.by("version").descending());

        var entityPage = springDataRubricVersionRepository.findAllByRubricIdAndStatus(rubricId, status, pageable);

        // Giả sử bạn có RubricVersionMapper để chuyển Entity -> Domain
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

        Pageable springPageable = PageRequest.of(page, size);

        // 1. Thực thi lấy dữ liệu từ Database
        var pageEntity = springDataRubricVersionRepository.searchRubricVersions(
                rubricId, StringNormalization.toLikePattern(keyword), status, springPageable);

        // 2. Map Entity về Domain và trả ra ngoài
        return new PageResult<>(
                pageEntity.getContent().stream()
                        .map(RubricVersionMapper::toDomain) // Chuyển đổi sang Domain Model
                        .toList(),
                pageEntity.getNumber(),
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    @Override
    public int archiveExpiredPublishedVersions(OffsetDateTime now) {
        return springDataRubricVersionRepository.archiveExpiredPublished(now);
    }
}


