package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.rubric.RubricResultBandJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
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
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricResultBandMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricResultBandRepository;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

@Repository
@RequiredArgsConstructor
public class RubricResultBandRepositoryImpl implements RubricResultBandRepository {

    private final SpringDataRubricResultBandRepository springDataRubricResultBandRepository;

    @Override
    public Optional<RubricResultBand> findById(UUID id) {
        return springDataRubricResultBandRepository.findById(id).map(RubricResultBandMapper::toDomain);
    }

    @Override
    public List<RubricResultBand> findByIdIn(Collection<UUID> ids) {
        return springDataRubricResultBandRepository.findByIdIn(ids).stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }

    @Override
    public RubricResultBand save(RubricResultBand band) {
        RubricResultBandJpaEntity entity = RubricResultBandMapper.toJpa(band);
        RubricResultBandJpaEntity saved = springDataRubricResultBandRepository.save(entity);
        return RubricResultBandMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRubricResultBandRepository.deleteById(id);
    }

    @Override
    public void deleteByRubricVersionId(UUID rubricVersionId) {
         springDataRubricResultBandRepository.deleteByRubricVersionId(rubricVersionId);
    }

    @Override
    public List<RubricResultBand> saveAll(List<RubricResultBand> bands) {
        List<RubricResultBandJpaEntity> entities = bands.stream()
                .map(RubricResultBandMapper::toJpa)
                .toList();
        List<RubricResultBandJpaEntity> savedEntities = DataConflictTranslation.translating(() -> springDataRubricResultBandRepository.saveAll(entities));

        return savedEntities.stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }

    @Override
    public void updateResultBandAtomic(UUID id, String code, String name, String description, boolean descriptionProvided, BigDecimal scoreMin, BigDecimal scoreMax, Integer order, Instant updatedAt, String updatedBy) {
        DataConflictTranslation.translating(() -> springDataRubricResultBandRepository.updateResultBandAtomic(id, code, name, description, descriptionProvided, scoreMin, scoreMax, order, updatedAt, updatedBy));
    }

    @Override
    public PageResult<RubricResultBand> findAllByRubricVersionId(UUID rubricVersionId, int page, int size) {
        // Optimization: sort by order ASCENDING by default so the result table is displayed in the right order
        Pageable pageable = OneBasedPaging.toPageRequest(page, size, Sort.by("order").ascending());

        Page<RubricResultBandJpaEntity> entityPage = springDataRubricResultBandRepository.findAllByRubricVersionId(rubricVersionId, pageable);

        List<RubricResultBand> bands = entityPage.getContent().stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();

        return OneBasedPaging.toPageResult(entityPage, bands);
    }

    @Override
    public List<RubricResultBand> findByRubricVersionIdIn(List<UUID> versionIds) {
        return springDataRubricResultBandRepository.findByRubricVersionIdIn(versionIds)
                .stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<RubricResultBand> searchRubricResultBands(UUID versionId, String keyword, int page, int size) {
        PageRequest springPageable = OneBasedPaging.toPageRequest(page, size);
        Page<RubricResultBandJpaEntity> pageEntity = springDataRubricResultBandRepository.searchRubricResultBands(
                versionId, StringNormalization.toLikePattern(keyword), springPageable);

        return OneBasedPaging.toPageResult(pageEntity, pageEntity.getContent().stream().map(RubricResultBandMapper::toDomain).toList());
    }

    @Override
    public List<RubricResultBand> findByRubricVersionId(UUID rubricVersionId) {
        return springDataRubricResultBandRepository.findByRubricVersionId(rubricVersionId)
                .stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }
}


