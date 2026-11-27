package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.rubric.RubricCriterionJpaEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionMapper;
import com.sep.vox.infrastructure.persistence.audit.AuditStampProvider;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricCriterionRepository;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

@Repository
@RequiredArgsConstructor
public class RubricCriterionRepositoryImpl implements RubricCriterionRepository {

    private final SpringDataRubricCriterionRepository springDataRubricCriterionRepository;
    private final AuditStampProvider auditStampProvider;

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
        DataConflictTranslation.translating(() -> springDataRubricCriterionRepository.saveAll(entities));
    }

    @Override
    public void updateCriterionAtomic(UUID id, String code, String name, String description, boolean descriptionProvided, String examplesJson, boolean examplesJsonProvided, BigDecimal weight, BigDecimal minScore, BigDecimal maxScore, Integer order, Boolean isRequired) {
        DataConflictTranslation.translating(() -> springDataRubricCriterionRepository.updateCriterionAtomic(id, code, name, description, descriptionProvided, examplesJson, examplesJsonProvided, weight, minScore, maxScore, order, isRequired,
                auditStampProvider.currentUpdatedAt(), auditStampProvider.currentUpdatedBy()));
    }

    @Override
    public PageResult<RubricCriterion> findAllByRubricVersionId(UUID rubricVersionId, int page, int size) {
        // Optimization: sort by order by default so the scoring table is displayed in the right order
        Pageable pageable = OneBasedPaging.toPageRequest(page, size, Sort.by("order").ascending());

        Page<RubricCriterionJpaEntity> entityPage = springDataRubricCriterionRepository.findAllByRubricVersionId(rubricVersionId, pageable);

        List<RubricCriterion> criteria = entityPage.getContent().stream()
                .map(RubricCriterionMapper::toDomain)
                .toList();

        return OneBasedPaging.toPageResult(entityPage, criteria);
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
        PageRequest springPageable = OneBasedPaging.toPageRequest(page, size);
        Page<RubricCriterionJpaEntity> pageEntity = springDataRubricCriterionRepository.searchRubricCriteria(
                versionId, StringNormalization.toLikePattern(keyword), isRequired, springPageable);

        return OneBasedPaging.toPageResult(pageEntity, pageEntity.getContent().stream().map(RubricCriterionMapper::toDomain).toList());
    }

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
