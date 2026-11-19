package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.RubricCriterionJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricCriterionRepository;

@Repository
public class RubricCriterionRepositoryImpl implements RubricCriterionRepository {

    private final SpringDataRubricCriterionRepository springDataRubricCriterionRepository;

    public RubricCriterionRepositoryImpl(SpringDataRubricCriterionRepository springDataRubricCriterionRepository) {
        this.springDataRubricCriterionRepository = springDataRubricCriterionRepository;
    }

    @Override
    public Optional<RubricCriterion> findById(UUID id) {
        return springDataRubricCriterionRepository.findById(id).map(RubricCriterionMapper::toDomain);
    }

    @Override
    public RubricCriterion save(RubricCriterion criterion) {
        RubricCriterionJpaEntity entity = RubricCriterionMapper.toJpa(criterion);
        RubricCriterionJpaEntity saved = springDataRubricCriterionRepository.save(entity);
        return RubricCriterionMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
            springDataRubricCriterionRepository.deleteById(id);
    }

    @Override
    public void deleteByRubricVersionId(UUID rubricVersionId) {
        springDataRubricCriterionRepository.deleteByRubricVersionId(rubricVersionId);
    }

    @Override
    public void saveAll(List<RubricCriterion> criteria) {
        List<RubricCriterionJpaEntity> entities = criteria.stream()
                .map(RubricCriterionMapper::toJpa)
                .toList();
        springDataRubricCriterionRepository.saveAll(entities);
    }

    @Override
    public void updateCriterionAtomic(UUID id, String code, String name, String description, String examplesJson, BigDecimal weight, BigDecimal minScore, BigDecimal maxScore, Integer order, Boolean isRequired, Instant updatedAt, UUID updatedBy) {
        springDataRubricCriterionRepository.updateCriterionAtomic(id, code, name, description, examplesJson, weight, minScore, maxScore, order, isRequired, updatedAt, updatedBy);
    }

    @Override
    public PageResult<RubricCriterion> findAllByRubricVersionId(UUID rubricVersionId, int page, int size) {
        // Optimization: sort by order by default so the scoring table is displayed in the right order
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("order").ascending());

        Page<RubricCriterionJpaEntity> entityPage = springDataRubricCriterionRepository.findAllByRubricVersionId(rubricVersionId, pageable);

        List<RubricCriterion> criteria = entityPage.getContent().stream()
                .map(RubricCriterionMapper::toDomain)
                .toList();

        return new PageResult<>(
                criteria,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public List<RubricCriterion> findByRubricVersionIdIn(List<UUID> versionIds) {
        return springDataRubricCriterionRepository.findByRubricVersionIdIn(versionIds)
                .stream()
                .map(RubricCriterionMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<RubricCriterion> searchRubricCriteria(UUID versionId, String keyword, Boolean isRequired, int page, int size) {
        PageRequest springPageable = PageRequest.of(page - 1, size);
        Page<RubricCriterionJpaEntity> pageEntity = springDataRubricCriterionRepository.searchRubricCriteria(
                versionId, StringNormalization.toLikePattern(keyword), isRequired, springPageable);

        return new PageResult<>(
                pageEntity.getContent().stream().map(RubricCriterionMapper::toDomain).toList(),
                pageEntity.getNumber(),
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    // Ném thêm override này vào trong class RubricCriterionRepositoryImpl
    @Override
    public List<RubricCriterion> findByRubricVersionId(UUID rubricVersionId) {
        return springDataRubricCriterionRepository.findByRubricVersionId(rubricVersionId)
                .stream()
                .map(RubricCriterionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RubricCriterion> findByRubricVersionIdAndCode(UUID rubricVersionId, String code) {
        return springDataRubricCriterionRepository.findByRubricVersionIdAndCode(rubricVersionId, code)
                .map(RubricCriterionMapper::toDomain);
    }

    @Override
    public Optional<RubricCriterion> findByRubricVersionIdAndCodeIgnoreCase(UUID rubricVersionId, String code) {
        return springDataRubricCriterionRepository
                .findByRubricVersionIdAndCodeIgnoreCase(rubricVersionId, code)
                .map(RubricCriterionMapper::toDomain);
    }
}
