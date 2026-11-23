package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentGradingAssignmentJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentGradingAssignmentMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentGradingAssignmentRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentGradingAssignmentRepositoryImpl implements AssessmentGradingAssignmentRepository {

    private final SpringDataAssessmentGradingAssignmentRepository springDataAssessmentGradingAssignmentRepository;

    @Override
    public Optional<AssessmentGradingAssignment> findById(UUID id) {
        return springDataAssessmentGradingAssignmentRepository.findById(id)
            .map(AssessmentGradingAssignmentMapper::toDomain);
    }

    @Override
    public Optional<AssessmentGradingAssignment> findByIdForUpdate(UUID id) {
        return springDataAssessmentGradingAssignmentRepository.findByIdForUpdate(id)
            .map(AssessmentGradingAssignmentMapper::toDomain);
    }

    @Override
    public Optional<AssessmentGradingAssignment> findOpenByCandidateResultId(UUID candidateResultId) {
        return springDataAssessmentGradingAssignmentRepository.findByActiveResultId(candidateResultId)
            .map(AssessmentGradingAssignmentMapper::toDomain);
    }

    @Override
    public List<AssessmentGradingAssignment> findOpenByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentGradingAssignmentRepository.findByActiveResultIdIn(candidateResultIds).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentGradingAssignment> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId) {
        return springDataAssessmentGradingAssignmentRepository
            .findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentGradingAssignmentRepository.findByCandidateResultIdIn(candidateResultIds).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByCandidateResultIdAndTeacherId(UUID candidateResultId, UUID teacherId) {
        return springDataAssessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, teacherId);
    }

    @Override
    public List<AssessmentGradingAssignment> findByAppealId(UUID appealId) {
        return springDataAssessmentGradingAssignmentRepository.findByAppealId(appealId).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentGradingAssignment> findOverdue(Instant now) {
        return springDataAssessmentGradingAssignmentRepository.findOverdue(now).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentGradingAssignment> findOverdueInSchool(
            Instant now, UUID schoolId, UUID assessmentId, int limit) {
        return springDataAssessmentGradingAssignmentRepository
            .findOverdueInSchool(now, schoolId, assessmentId, PageRequest.of(0, limit)).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentGradingAssignment> findDueForReminder(Instant threshold) {
        return springDataAssessmentGradingAssignmentRepository.findDueForReminder(threshold).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
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
    public AssessmentGradingAssignment save(AssessmentGradingAssignment assignment) {
        AssessmentGradingAssignmentJpaEntity saved = springDataAssessmentGradingAssignmentRepository.saveAndFlush(
            AssessmentGradingAssignmentMapper.toJpa(assignment));
        return AssessmentGradingAssignmentMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentGradingAssignment> saveAll(List<AssessmentGradingAssignment> assignments) {
        List<AssessmentGradingAssignmentJpaEntity> entities = assignments.stream().map(AssessmentGradingAssignmentMapper::toJpa).toList();
        return springDataAssessmentGradingAssignmentRepository.saveAllAndFlush(entities).stream()
            .map(AssessmentGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentGradingAssignmentRepository.deleteById(id);
    }

    @Override
    public void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return;
        }
        springDataAssessmentGradingAssignmentRepository.deleteByCandidateResultIdIn(candidateResultIds);
    }

    @Override
    public long countByTeacherIdAndStatus(UUID teacherId, GradingAssignmentStatus status) {
        return springDataAssessmentGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, status.name());
    }
}
