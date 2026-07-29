package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamBlueprintMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamBlueprintRepository;

@Repository
public class ExamBlueprintRepositoryImpl implements ExamBlueprintRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamBlueprintRepositoryImpl.class);

    private final SpringDataExamBlueprintRepository springDataExamBlueprintRepository;

    public ExamBlueprintRepositoryImpl(SpringDataExamBlueprintRepository springDataExamBlueprintRepository) {
        this.springDataExamBlueprintRepository = springDataExamBlueprintRepository;
    }

    @Override
    public ExamBlueprint save(ExamBlueprint blueprint) {
        var saved = springDataExamBlueprintRepository.save(ExamBlueprintMapper.toJpa(blueprint));
        return ExamBlueprintMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamBlueprint> findById(UUID id) {
        var startedAt = System.nanoTime();
        var result = springDataExamBlueprintRepository.findById(id)
            .map(ExamBlueprintMapper::toDomain);
        LOGGER.info("[blueprint-perf] repo ExamBlueprintRepositoryImpl.findById id={} tookMs={}",
            id, (System.nanoTime() - startedAt) / 1_000_000);
        return result;
    }

    @Override
    public List<ExamBlueprint> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataExamBlueprintRepository.findAllById(ids).stream()
            .map(ExamBlueprintMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<ExamBlueprint> findAccessible(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin, UUID schoolId, Boolean isActive, UUID languageId, String examKind, String keyword, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = springDataExamBlueprintRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            schoolId,
            isActive,
            languageId,
            examKind,
            StringNormalization.toLikePattern(keyword),
            pageable
        );
        return new PageResult<>(
            result.getContent().stream().map(ExamBlueprintMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public boolean existsUsedByExam(UUID blueprintId) {
        return springDataExamBlueprintRepository.existsUsedByExam(blueprintId);
    }

    @Override
    public boolean canEditBlueprint(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataExamBlueprintRepository.canEditBlueprint(blueprintId, userId, schoolId);
    }

    @Override
    public boolean canChangeVersionStatus(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataExamBlueprintRepository.canChangeVersionStatus(blueprintId, userId, schoolId);
    }

    @Override
    public boolean canViewBlueprint(UUID blueprintId, UUID userId, UUID schoolId) {
        return springDataExamBlueprintRepository.canViewBlueprint(blueprintId, userId, schoolId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamBlueprintRepository.deleteById(id);
    }
}
