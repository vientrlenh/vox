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
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricResultBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricResultBandRepository;

@Repository
public class RubricResultBandRepositoryImpl implements RubricResultBandRepository {

    private final SpringDataRubricResultBandRepository springDataRubricResultBandRepository;

    public RubricResultBandRepositoryImpl(SpringDataRubricResultBandRepository springDataRubricResultBandRepository) {
        this.springDataRubricResultBandRepository = springDataRubricResultBandRepository;
    }

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
        var entity = RubricResultBandMapper.toJpa(band);
        var saved = springDataRubricResultBandRepository.save(entity);
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
        var entities = bands.stream()
                .map(RubricResultBandMapper::toJpa)
                .toList();
        var savedEntities = springDataRubricResultBandRepository.saveAll(entities);

        return savedEntities.stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }

    @Override
    public void updateResultBandAtomic(UUID id, String code, String name, String description, BigDecimal scoreMin, BigDecimal scoreMax, Integer order, OffsetDateTime updatedAt, UUID updatedBy) {
        springDataRubricResultBandRepository.updateResultBandAtomic(id, code, name, description, scoreMin, scoreMax, order, updatedAt, updatedBy);
    }

    @Override
    public PageResult<RubricResultBand> findAllByRubricVersionId(UUID rubricVersionId, int page, int size) {
        // Tối ưu: Mặc định sort theo order TĂNG DẦN để bảng kết quả hiển thị chuẩn
        Pageable pageable = PageRequest.of(page, size, Sort.by("order").ascending());

        var entityPage = springDataRubricResultBandRepository.findAllByRubricVersionId(rubricVersionId, pageable);

        List<RubricResultBand> bands = entityPage.getContent().stream()
                .map(RubricResultBandMapper::toDomain)
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
    public List<RubricResultBand> findByRubricVersionIdIn(List<UUID> versionIds) {
        return springDataRubricResultBandRepository.findByRubricVersionIdIn(versionIds)
                .stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<RubricResultBand> searchRubricResultBands(UUID versionId, String keyword, int page, int size) {
        var springPageable = PageRequest.of(page, size);
        var pageEntity = springDataRubricResultBandRepository.searchRubricResultBands(
                versionId, StringNormalization.toLikePattern(keyword), springPageable);

        return new PageResult<>(
                pageEntity.getContent().stream().map(RubricResultBandMapper::toDomain).toList(),
                pageEntity.getNumber(),
                pageEntity.getSize(),
                (int) pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    @Override
    public List<RubricResultBand> findByRubricVersionId(UUID rubricVersionId) {
        return springDataRubricResultBandRepository.findByRubricVersionId(rubricVersionId)
                .stream()
                .map(RubricResultBandMapper::toDomain)
                .toList();
    }
}


