package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentCandidateMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentCandidateRepository;

@Repository
public class AssessmentCandidateRepositoryImpl implements AssessmentCandidateRepository {

    private final SpringDataAssessmentCandidateRepository springDataAssessmentCandidateRepository;

    public AssessmentCandidateRepositoryImpl(SpringDataAssessmentCandidateRepository springDataAssessmentCandidateRepository) {
        this.springDataAssessmentCandidateRepository = springDataAssessmentCandidateRepository;
    }

    @Override
    public AssessmentCandidate save(AssessmentCandidate candidate) {
        try {
            var saved = springDataAssessmentCandidateRepository.save(AssessmentCandidateMapper.toJpa(candidate));
            return AssessmentCandidateMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceDuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }
    }

    @Override
    public List<AssessmentCandidate> saveAll(Collection<AssessmentCandidate> candidates) {
        try {
            var entities = candidates.stream().map(AssessmentCandidateMapper::toJpa).toList();
            return springDataAssessmentCandidateRepository.saveAll(entities).stream()
                .map(AssessmentCandidateMapper::toDomain)
                .toList();
        } catch (DataIntegrityViolationException e) {
            throw new ResourceDuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }
    }

    @Override
    public List<AssessmentCandidate> findByExamId(UUID examId) {
        return springDataAssessmentCandidateRepository.findByExamIdOrderByAssignedAtAscIdAsc(examId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidate> findByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentCandidateRepository.findByExamIdIn(examIds).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public long countByExamId(UUID examId) {
        return springDataAssessmentCandidateRepository.countByExamId(examId);
    }

    @Override
    public Map<UUID, Long> countByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return Map.of();
        }
        return springDataAssessmentCandidateRepository.countByExamIdIn(examIds).stream()
            .collect(Collectors.toMap(
                count -> count.getExamId(),
                count -> count.getCandidateCount()
            ));
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentCandidateRepository.deleteById(id);
    }

    @Override
    public void deleteByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        springDataAssessmentCandidateRepository.deleteByIdIn(ids);
    }

    @Override
    public void deleteByExamId(UUID examId) {
        springDataAssessmentCandidateRepository.deleteByExamId(examId);
    }

    public Optional<AssessmentCandidate> findById(UUID id) {
        return springDataAssessmentCandidateRepository.findById(id)
            .map(AssessmentCandidateMapper::toDomain);
    }

    @Override
    public Optional<AssessmentCandidate> findByExamIdAndStudentId(UUID examId, UUID studentId) {
        return springDataAssessmentCandidateRepository.findByExamIdAndStudentId(examId, studentId)
            .map(AssessmentCandidateMapper::toDomain);
    }

    @Override
    public boolean existsByExamIdAndStudentId(UUID examId, UUID studentId) {
        return springDataAssessmentCandidateRepository.existsByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public List<AssessmentCandidate> findByStudentId(UUID studentId) {
        return springDataAssessmentCandidateRepository.findByStudentId(studentId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidate> findByScheduleId(UUID scheduleId) {
        return springDataAssessmentCandidateRepository.findByScheduleId(scheduleId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidate> findByAssignedPaperId(UUID paperId) {
        return springDataAssessmentCandidateRepository.findByAssignedPaperId(paperId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public Set<UUID> findStudentIdsByExamId(UUID examId) {
        return new HashSet<>(springDataAssessmentCandidateRepository.findStudentIdsByExamId(examId));
    }

    @Override
    public List<UUID> findUnblockedStudentIdsByExamId(UUID examId) {
        return springDataAssessmentCandidateRepository.findDistinctUnblockedStudentIdsByExamId(examId);
    }

    @Override
    public List<AssessmentCandidate> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId) {
        return springDataAssessmentCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidate> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentCandidateRepository.findByIdIn(ids).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidate> findByIdInAndExamId(Collection<UUID> ids, UUID examId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentCandidateRepository.findByIdInAndExamId(ids, examId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public long countByScheduleId(UUID scheduleId) {
        return springDataAssessmentCandidateRepository.countByScheduleId(scheduleId);
    }

    @Override
    public boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId) {
        return springDataAssessmentCandidateRepository.existsByScheduleIdAndStudentId(scheduleId, studentId);
    }

    @Override
    public boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId) {
        return springDataAssessmentCandidateRepository.existsByExamIdAndScheduleIdIsNotNull(examId);
    }

    @Override
    public Optional<AssessmentCandidate> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId) {
        return springDataAssessmentCandidateRepository.findByScheduleIdAndStudentId(scheduleId, studentId).map(AssessmentCandidateMapper::toDomain);
    }

    @Override
    public List<AssessmentCandidate> findActiveCandidates(UUID studentId, Instant now) {
        return springDataAssessmentCandidateRepository.findActiveCandidate(studentId, now)
            .stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<StudentScheduleConflict> findConflictsForStudents(
            Collection<UUID> studentIds, Instant start, Instant end, UUID excludeScheduleId) {
        // Ca thi chưa đặt giờ thì không có gì để so — coi như không vướng, đúng như cách
        // ExamScheduleRoomValidator bỏ qua khi thiếu dữ liệu.
        if (studentIds == null || studentIds.isEmpty() || start == null || end == null) {
            return List.of();
        }
        return springDataAssessmentCandidateRepository
            .findOverlappingScheduleAssignments(studentIds, start, end, excludeScheduleId).stream()
            .map(row -> new StudentScheduleConflict(
                (UUID) row[0], (UUID) row[1], (Instant) row[2], (Instant) row[3]))
            .toList();
    }
}

