package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamBlueprintVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamBlueprintVersionRepository;

@Repository
public class ExamBlueprintVersionRepositoryImpl implements ExamBlueprintVersionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamBlueprintVersionRepositoryImpl.class);

    private final SpringDataExamBlueprintVersionRepository springDataExamBlueprintVersionRepository;

    public ExamBlueprintVersionRepositoryImpl(SpringDataExamBlueprintVersionRepository springDataExamBlueprintVersionRepository) {
        this.springDataExamBlueprintVersionRepository = springDataExamBlueprintVersionRepository;
    }

    @Override
    public ExamBlueprintVersion save(ExamBlueprintVersion version) {
        var saved = springDataExamBlueprintVersionRepository.save(ExamBlueprintVersionMapper.toJpa(version));
        return ExamBlueprintVersionMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamBlueprintVersion> findById(UUID id) {
        var startedAt = System.nanoTime();
        var result = springDataExamBlueprintVersionRepository.findById(id)
            .map(ExamBlueprintVersionMapper::toDomain);
        LOGGER.info("[blueprint-perf] repo ExamBlueprintVersionRepositoryImpl.findById id={} tookMs={}",
            id, (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<ExamBlueprintVersion> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataExamBlueprintVersionRepository.findAllById(ids).stream()
            .map(ExamBlueprintVersionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamBlueprintVersion> findByBlueprintId(UUID blueprintId) {
        return springDataExamBlueprintVersionRepository.findByBlueprintIdOrderByVersionDesc(blueprintId).stream()
            .map(ExamBlueprintVersionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamBlueprintVersion> findByBlueprintIdIn(Collection<UUID> blueprintIds) {
        if (blueprintIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataExamBlueprintVersionRepository.findByBlueprintIdInOrderByVersionDesc(blueprintIds).stream()
            .map(ExamBlueprintVersionMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo ExamBlueprintVersionRepositoryImpl.findByBlueprintIdIn blueprintIds={} tookMs={}",
            blueprintIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<ExamBlueprintVersion> findByBlueprintIdAndStatus(UUID blueprintId, ExamBlueprintVersionStatus status) {
        return springDataExamBlueprintVersionRepository.findByBlueprintIdAndStatusOrderByVersionDesc(
            blueprintId,
            status.name()
        ).stream().map(ExamBlueprintVersionMapper::toDomain).toList();
    }

    @Override
    public int nextVersionNumber(UUID blueprintId) {
        return springDataExamBlueprintVersionRepository.nextVersionNumber(blueprintId);
    }

    @Override
    public boolean existsUsedByVersion(UUID versionId) {
        return springDataExamBlueprintVersionRepository.existsUsedByVersion(versionId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamBlueprintVersionRepository.deleteById(id);
    }
}
