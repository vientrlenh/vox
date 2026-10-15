package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentRepository;

@Repository
public class AssessmentRepositoryImpl implements AssessmentRepository {

    private final SpringDataAssessmentRepository springDataAssessmentRepository;

    public AssessmentRepositoryImpl(SpringDataAssessmentRepository springDataAssessmentRepository) {
        this.springDataAssessmentRepository = springDataAssessmentRepository;
    }

    @Override
    public Optional<Assessment> findById(UUID id) {
        return springDataAssessmentRepository.findById(id)
            .map(AssessmentMapper::toDomain);
    }

    @Override
    public List<Assessment> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentRepository.findAllById(ids).stream()
            .map(AssessmentMapper::toDomain)
            .toList();
    }

    @Override
    public Assessment save(Assessment exam) {
        var entity = AssessmentMapper.toJpa(exam);
        var saved = springDataAssessmentRepository.save(entity);
        return AssessmentMapper.toDomain(saved);
    }

    @Override
    public PageResult<Assessment> findAccessible(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin, UUID schoolId, UUID schoolClassId, AssessmentKind kind, AssessmentStatus status, String keyword,
            int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        var result = springDataAssessmentRepository.findAccessible(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            schoolId,
            schoolClassId,
            kind == null ? null : kind.name(),
            status == null ? null : status.name(),
            StringNormalization.toLikePattern(keyword),
            pageable
        );
        return new PageResult<>(
            result.getContent().stream().map(AssessmentMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<Assessment> findAllByBlueprintId(UUID blueprintId) {
        return springDataAssessmentRepository.findAllByBlueprintId(blueprintId).stream()
            .map(AssessmentMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByBlueprintId(UUID blueprintId) {
        return springDataAssessmentRepository.existsByBlueprintId(blueprintId);
    }

    @Override
    public boolean existsByBlueprintIdAndKindAndStatusNot(UUID blueprintId, AssessmentKind kind, AssessmentStatus status) {
        return springDataAssessmentRepository.existsByBlueprintIdAndKindAndStatusNot(blueprintId, kind.name(), status.name());
    }

    @Override
    public boolean existsSubmittedSessionByExamId(UUID examId) {
        return springDataAssessmentRepository.existsSubmittedSessionByExamId(examId);
    }

    @Override
    public List<Assessment> findByStatusAndOpenAtBefore(AssessmentStatus status, Instant time) {
        return springDataAssessmentRepository.findByStatusAndOpenAtBefore(status.name(), time).stream()
            .map(AssessmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<Assessment> findDueForHumanGradingNotice() {
        return springDataAssessmentRepository.findDueForHumanGradingNotice().stream()
            .map(AssessmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<Assessment> findByStatusAndCloseAtBefore(AssessmentStatus status, Instant time) {
        return springDataAssessmentRepository.findByStatusAndCloseAtBefore(status.name(), time).stream()
            .map(AssessmentMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentRepository.deleteById(id);
    }
}
