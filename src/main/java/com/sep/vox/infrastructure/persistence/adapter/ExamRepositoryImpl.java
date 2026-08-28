package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamRepository;

@Repository
public class ExamRepositoryImpl implements ExamRepository {

    private final SpringDataExamRepository springDataExamRepository;

    public ExamRepositoryImpl(SpringDataExamRepository springDataExamRepository) {
        this.springDataExamRepository = springDataExamRepository;
    }

    @Override
    public Optional<Exam> findById(UUID id) {
        return springDataExamRepository.findById(id)
            .map(ExamMapper::toDomain);
    }

    @Override
    public List<Exam> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataExamRepository.findAllById(ids).stream()
            .map(ExamMapper::toDomain)
            .toList();
    }

    @Override
    public Exam save(Exam exam) {
        var entity = ExamMapper.toJpa(exam);
        var saved = springDataExamRepository.save(entity);
        return ExamMapper.toDomain(saved);
    }

    @Override
    public PageResult<Exam> findAccessible(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin,
            boolean schoolAdmin, UUID schoolId, UUID schoolClassId, ExamKind kind, ExamStatus status, String keyword,
            int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        var result = springDataExamRepository.findAccessible(
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
            result.getContent().stream().map(ExamMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<Exam> findAllByBlueprintId(UUID blueprintId) {
        return springDataExamRepository.findAllByBlueprintId(blueprintId).stream()
            .map(ExamMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByBlueprintId(UUID blueprintId) {
        return springDataExamRepository.existsByBlueprintId(blueprintId);
    }

    @Override
    public boolean existsByBlueprintIdAndKindAndStatusNot(UUID blueprintId, ExamKind kind, ExamStatus status) {
        return springDataExamRepository.existsByBlueprintIdAndKindAndStatusNot(blueprintId, kind.name(), status.name());
    }

    @Override
    public boolean existsSubmittedSessionByExamId(UUID examId) {
        return springDataExamRepository.existsSubmittedSessionByExamId(examId);
    }

    @Override
    public List<Exam> findByStatusAndOpenAtBefore(ExamStatus status, Instant time) {
        return springDataExamRepository.findByStatusAndOpenAtBefore(status.name(), time).stream()
            .map(ExamMapper::toDomain)
            .toList();
    }

    @Override
    public List<Exam> findByStatusAndCloseAtBefore(ExamStatus status, Instant time) {
        return springDataExamRepository.findByStatusAndCloseAtBefore(status.name(), time).stream()
            .map(ExamMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamRepository.deleteById(id);
    }
}
