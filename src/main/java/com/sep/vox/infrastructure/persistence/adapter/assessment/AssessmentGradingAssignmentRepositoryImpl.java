package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentGradingAssignmentMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentGradingAssignmentRepository;

@Repository
public class AssessmentGradingAssignmentRepositoryImpl implements AssessmentGradingAssignmentRepository {

    private final SpringDataAssessmentGradingAssignmentRepository springDataAssessmentGradingAssignmentRepository;

    public AssessmentGradingAssignmentRepositoryImpl(
            SpringDataAssessmentGradingAssignmentRepository springDataAssessmentGradingAssignmentRepository) {
        this.springDataAssessmentGradingAssignmentRepository = springDataAssessmentGradingAssignmentRepository;
    }

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
            Instant now, UUID schoolId, UUID examId, int limit) {
        return springDataAssessmentGradingAssignmentRepository
            .findOverdueInSchool(now, schoolId, examId, PageRequest.of(0, limit)).stream()
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
     * Flush ngay, không đợi cuối transaction — bỏ đi là lỗi thật, không phải tối ưu.
     *
     * <p>Unique index trên {@code active_result_id} nhạy với THỨ TỰ ghi. Hibernate mặc
     * định flush INSERT trước UPDATE, nên "đóng vòng cũ rồi mở vòng mới cho cùng một
     * bài" (chính là {@code CLEAR_INVALID}) sẽ chèn dòng mới trước khi nhả
     * {@code active_result_id} của dòng cũ -> vi phạm unique.
     */
    @Override
    public AssessmentGradingAssignment save(AssessmentGradingAssignment assignment) {
        var saved = springDataAssessmentGradingAssignmentRepository.saveAndFlush(
            AssessmentGradingAssignmentMapper.toJpa(assignment));
        return AssessmentGradingAssignmentMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentGradingAssignment> saveAll(List<AssessmentGradingAssignment> assignments) {
        var entities = assignments.stream().map(AssessmentGradingAssignmentMapper::toJpa).toList();
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
    public long countByTeacherIdAndStatus(UUID teacherId, AssessmentGradingAssignmentStatus status) {
        return springDataAssessmentGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, status.name());
    }
}
