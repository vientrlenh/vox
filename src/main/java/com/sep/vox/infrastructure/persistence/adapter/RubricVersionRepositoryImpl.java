package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
    public PageResult<RubricVersion> findAllByRubricIdAndStatus(UUID rubricId,String status, int page, int size) {
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
}


