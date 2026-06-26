package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.RubricCriterionBand;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricCriterionBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricCriterionBandRepository;

@Repository
public class RubricCriterionBandRepositoryImpl implements RubricCriterionBandRepository {

    private final SpringDataRubricCriterionBandRepository springDataRubricCriterionBandRepository;

    public RubricCriterionBandRepositoryImpl(SpringDataRubricCriterionBandRepository springDataRubricCriterionBandRepository) {
        this.springDataRubricCriterionBandRepository = springDataRubricCriterionBandRepository;
    }

    @Override
    public Optional<RubricCriterionBand> findById(UUID id) {
        return springDataRubricCriterionBandRepository.findById(id).map(RubricCriterionBandMapper::toDomain);
    }

    @Override
    public RubricCriterionBand save(RubricCriterionBand band) {
        var entity = RubricCriterionBandMapper.toJpa(band);
        var saved = springDataRubricCriterionBandRepository.save(entity);
        return RubricCriterionBandMapper.toDomain(saved);
    }

    @Override
    public void deleteByCriterionId(UUID rubricCriterionId) {
        springDataRubricCriterionBandRepository.deleteByCriterionId(rubricCriterionId);
    }

    @Override
    public void deleteByRubricVersionId(UUID rubricVersionId) {
        springDataRubricCriterionBandRepository.deleteByRubricVersionId(rubricVersionId);
    }

    @Override
    public void saveAll(List<RubricCriterionBand> rubricCriterionBands) {
        var entities = rubricCriterionBands.stream()
                .map(RubricCriterionBandMapper::toJpa)
                .toList();
        springDataRubricCriterionBandRepository.saveAll(entities);
    }

    @Override
    public void deleteById(UUID id) {
        springDataRubricCriterionBandRepository.deleteById(id);
    }

    @Override
    public void updateBandAtomic(UUID id, String code, BigDecimal scoreMin, BigDecimal scoreMax, OffsetDateTime updatedAt, UUID updatedBy) {
        springDataRubricCriterionBandRepository.updateBandAtomic(id, code, scoreMin, scoreMax, updatedAt, updatedBy);
    }

    @Override
    public PageResult<RubricCriterionBand> findAllByCriterionId(UUID criterionId, int page, int size) {
        // Tối ưu: Mặc định sort theo scoreMax GIẢM DẦN (Điểm cao nằm trên cùng, điểm thấp nằm dưới)
        Pageable pageable = PageRequest.of(page, size, Sort.by("scoreMax").descending());

        var entityPage = springDataRubricCriterionBandRepository.findAllByCriterionId(criterionId, pageable);

        List<RubricCriterionBand> bands = entityPage.getContent().stream()
                .map(RubricCriterionBandMapper::toDomain)
                .toList();

        return new PageResult<>(
                bands,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }


    @Override
    public List<RubricCriterionBand> findByCriterionIdIn(List<UUID> criterionIds) {
        return springDataRubricCriterionBandRepository.findByCriterionIdIn(criterionIds)
                .stream()
                .map(RubricCriterionBandMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<RubricCriterionBand> searchRubricCriterionBands(UUID criterionId, String keyword, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var pageEntity = springDataRubricCriterionBandRepository.searchRubricCriterionBands(criterionId, keyword, pageable);

        return new PageResult<>(
                pageEntity.getContent().stream().map(RubricCriterionBandMapper::toDomain).toList(),
                pageEntity.getNumber(),
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }
}
