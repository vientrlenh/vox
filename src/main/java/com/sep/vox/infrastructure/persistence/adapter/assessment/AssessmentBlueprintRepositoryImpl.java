package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentBlueprintMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentBlueprintRepository;

@Repository
public class AssessmentBlueprintRepositoryImpl implements AssessmentBlueprintRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentBlueprintRepositoryImpl.class);

    private final SpringDataAssessmentBlueprintRepository springDataAssessmentBlueprintRepository;

    public AssessmentBlueprintRepositoryImpl(SpringDataAssessmentBlueprintRepository springDataAssessmentBlueprintRepository) {
        this.springDataAssessmentBlueprintRepository = springDataAssessmentBlueprintRepository;
    }

    @Override
    public AssessmentBlueprint save(AssessmentBlueprint blueprint) {
        var saved = springDataAssessmentBlueprintRepository.save(AssessmentBlueprintMapper.toJpa(blueprint));
        return AssessmentBlueprintMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentBlueprint> findById(UUID id) {
        var startedAt = System.nanoTime();
        var result = springDataAssessmentBlueprintRepository.findById(id)
            .map(AssessmentBlueprintMapper::toDomain);
        LOGGER.info("[blueprint-perf] repo AssessmentBlueprintRepositoryImpl.findById id={} tookMs={}",
            id, (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<AssessmentBlueprint> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentBlueprintRepository.findAllById(ids).stream()
            .map(AssessmentBlueprintMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<AssessmentBlueprint> findAccessible(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin, UUID schoolId, Boolean isActive, UUID languageId, String assessmentKind, String keyword, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        var result = springDataAssessmentBlueprintRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            schoolId,
            isActive,
            languageId,
            assessmentKind,
            StringNormalization.toLikePattern(keyword),
            pageable
        );
        return new PageResult<>(
            result.getContent().stream().map(AssessmentBlueprintMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public boolean existsUsedByAssessment(UUID blueprintId) {
        return springDataAssessmentBlueprintRepository.existsUsedByAssessment(blueprintId);
    }

    @Override
    public boolean canEditBlueprint(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataAssessmentBlueprintRepository.canEditBlueprint(blueprintId, userId, schoolId);
    }

    @Override
    public boolean canChangeVersionStatus(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataAssessmentBlueprintRepository.canChangeVersionStatus(blueprintId, userId, schoolId);
    }

    @Override
    public boolean isChairOfAssessmentUsingBlueprint(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataAssessmentBlueprintRepository.isChairOfAssessmentUsingBlueprint(blueprintId, userId, schoolId);
    }

    @Override
    public boolean canViewBlueprint(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataAssessmentBlueprintRepository.canViewBlueprint(blueprintId, userId, schoolId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentBlueprintRepository.deleteById(id);
    }
}
