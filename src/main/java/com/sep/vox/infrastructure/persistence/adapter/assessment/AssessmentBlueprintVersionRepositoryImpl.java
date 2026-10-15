package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentBlueprintVersionMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentBlueprintVersionRepository;

@Repository
public class AssessmentBlueprintVersionRepositoryImpl implements AssessmentBlueprintVersionRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentBlueprintVersionRepositoryImpl.class);

    private final SpringDataAssessmentBlueprintVersionRepository springDataAssessmentBlueprintVersionRepository;

    public AssessmentBlueprintVersionRepositoryImpl(SpringDataAssessmentBlueprintVersionRepository springDataAssessmentBlueprintVersionRepository) {
        this.springDataAssessmentBlueprintVersionRepository = springDataAssessmentBlueprintVersionRepository;
    }

    @Override
    public AssessmentBlueprintVersion save(AssessmentBlueprintVersion version) {
        var saved = springDataAssessmentBlueprintVersionRepository.save(AssessmentBlueprintVersionMapper.toJpa(version));
        return AssessmentBlueprintVersionMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentBlueprintVersion> findById(UUID id) {
        var startedAt = System.nanoTime();
        var result = springDataAssessmentBlueprintVersionRepository.findById(id)
            .map(AssessmentBlueprintVersionMapper::toDomain);
        LOGGER.info("[blueprint-perf] repo AssessmentBlueprintVersionRepositoryImpl.findById id={} tookMs={}",
            id, (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<AssessmentBlueprintVersion> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentBlueprintVersionRepository.findAllById(ids).stream()
            .map(AssessmentBlueprintVersionMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentBlueprintVersion> findByBlueprintId(UUID blueprintId) {
        return springDataAssessmentBlueprintVersionRepository.findByBlueprintIdOrderByVersionDesc(blueprintId).stream()
            .map(AssessmentBlueprintVersionMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentBlueprintVersion> findByBlueprintIdIn(Collection<UUID> blueprintIds) {
        if (blueprintIds.isEmpty()) {
            return List.of();
        }
        var startedAt = System.nanoTime();
        var result = springDataAssessmentBlueprintVersionRepository.findByBlueprintIdInOrderByVersionDesc(blueprintIds).stream()
            .map(AssessmentBlueprintVersionMapper::toDomain)
            .toList();
        LOGGER.info("[blueprint-perf] repo AssessmentBlueprintVersionRepositoryImpl.findByBlueprintIdIn blueprintIds={} tookMs={}",
            blueprintIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<AssessmentBlueprintVersion> findByBlueprintIdAndStatus(UUID blueprintId, AssessmentBlueprintVersionStatus status) {
        return springDataAssessmentBlueprintVersionRepository.findByBlueprintIdAndStatusOrderByVersionDesc(
            blueprintId,
            status.name()
        ).stream().map(AssessmentBlueprintVersionMapper::toDomain).toList();
    }

    @Override
    public int nextVersionNumber(UUID blueprintId) {
        return springDataAssessmentBlueprintVersionRepository.nextVersionNumber(blueprintId);
    }

    @Override
    public boolean existsUsedByVersion(UUID versionId) {
        return springDataAssessmentBlueprintVersionRepository.existsUsedByVersion(versionId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentBlueprintVersionRepository.deleteById(id);
    }
}
