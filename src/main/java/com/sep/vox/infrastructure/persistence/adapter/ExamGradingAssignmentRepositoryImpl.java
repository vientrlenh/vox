package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamGradingAssignmentMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamGradingAssignmentRepository;

@Repository
public class ExamGradingAssignmentRepositoryImpl implements ExamGradingAssignmentRepository {

    private final SpringDataExamGradingAssignmentRepository springDataExamGradingAssignmentRepository;

    public ExamGradingAssignmentRepositoryImpl(
            SpringDataExamGradingAssignmentRepository springDataExamGradingAssignmentRepository) {
        this.springDataExamGradingAssignmentRepository = springDataExamGradingAssignmentRepository;
    }

    @Override
    public Optional<ExamGradingAssignment> findById(UUID id) {
        return springDataExamGradingAssignmentRepository.findById(id)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public Optional<ExamGradingAssignment> findByIdForUpdate(UUID id) {
        return springDataExamGradingAssignmentRepository.findByIdForUpdate(id)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public Optional<ExamGradingAssignment> findOpenByCandidateResultId(UUID candidateResultId) {
        return springDataExamGradingAssignmentRepository.findByActiveResultId(candidateResultId)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public List<ExamGradingAssignment> findOpenByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataExamGradingAssignmentRepository.findByActiveResultIdIn(candidateResultIds).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId) {
        return springDataExamGradingAssignmentRepository
            .findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataExamGradingAssignmentRepository.findByCandidateResultIdIn(candidateResultIds).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByCandidateResultIdAndTeacherId(UUID candidateResultId, UUID teacherId) {
        return springDataExamGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, teacherId);
    }

    @Override
    public List<ExamGradingAssignment> findByAppealId(UUID appealId) {
        return springDataExamGradingAssignmentRepository.findByAppealId(appealId).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findOverdue(Instant now) {
        return springDataExamGradingAssignmentRepository.findOverdue(now).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findOverdueInSchool(
            Instant now, UUID schoolId, UUID examId, int limit) {
        return springDataExamGradingAssignmentRepository
            .findOverdueInSchool(now, schoolId, examId, PageRequest.of(0, limit)).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findDueForReminder(Instant threshold) {
        return springDataExamGradingAssignmentRepository.findDueForReminder(threshold).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    /**
     * Flush right away, do not wait for the end of the transaction — removing this is a real bug, not an optimization.
     *
     * <p>The unique index on {@code active_result_id} is sensitive to the ORDER of writes. Hibernate flushes
     * INSERT before UPDATE by default, so "close the old round and open a new round for the same
     * attempt" (that is {@code CLEAR_INVALID}) would insert the new row before releasing the
     * {@code active_result_id} of the old row -> unique violation.
     */
    @Override
    public ExamGradingAssignment save(ExamGradingAssignment assignment) {
        ExamGradingAssignmentJpaEntity saved = springDataExamGradingAssignmentRepository.saveAndFlush(
            ExamGradingAssignmentMapper.toJpa(assignment));
        return ExamGradingAssignmentMapper.toDomain(saved);
    }

    @Override
    public List<ExamGradingAssignment> saveAll(List<ExamGradingAssignment> assignments) {
        List<ExamGradingAssignmentJpaEntity> entities = assignments.stream().map(ExamGradingAssignmentMapper::toJpa).toList();
        return springDataExamGradingAssignmentRepository.saveAllAndFlush(entities).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamGradingAssignmentRepository.deleteById(id);
    }

    @Override
    public void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return;
        }
        springDataExamGradingAssignmentRepository.deleteByCandidateResultIdIn(candidateResultIds);
    }

    @Override
    public long countByTeacherIdAndStatus(UUID teacherId, GradingAssignmentStatus status) {
        return springDataExamGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, status.name());
    }
}
