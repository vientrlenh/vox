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
    public List<AssessmentCandidate> findByAssessmentId(UUID assessmentId) {
        return springDataAssessmentCandidateRepository.findByAssessmentIdOrderByAssignedAtAscIdAsc(assessmentId).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidate> findByAssessmentIdIn(Collection<UUID> assessmentIds) {
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentCandidateRepository.findByAssessmentIdIn(assessmentIds).stream()
            .map(AssessmentCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public long countByAssessmentId(UUID assessmentId) {
        return springDataAssessmentCandidateRepository.countByAssessmentId(assessmentId);
    }

    @Override
    public Map<UUID, Long> countByAssessmentIdIn(Collection<UUID> assessmentIds) {
        if (assessmentIds.isEmpty()) {
            return Map.of();
        }
        return springDataAssessmentCandidateRepository.countByAssessmentIdIn(assessmentIds).stream()
            .collect(Collectors.toMap(
                count -> count.getAssessmentId(),
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
    public void deleteByAssessmentId(UUID assessmentId) {
        springDataAssessmentCandidateRepository.deleteByAssessmentId(assessmentId);
    }

    public Optional<AssessmentCandidate> findById(UUID id) {
        return springDataAssessmentCandidateRepository.findById(id)
            .map(AssessmentCandidateMapper::toDomain);
    }

    @Override
    public Optional<AssessmentCandidate> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId) {
        return springDataAssessmentCandidateRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)
            .map(AssessmentCandidateMapper::toDomain);
    }

    @Override
    public boolean existsByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId) {
        return springDataAssessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessmentId, studentId);
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
    public Set<UUID> findStudentIdsByAssessmentId(UUID assessmentId) {
        return new HashSet<>(springDataAssessmentCandidateRepository.findStudentIdsByAssessmentId(assessmentId));
    }

    @Override
    public List<UUID> findUnblockedStudentIdsByAssessmentId(UUID assessmentId) {
        return springDataAssessmentCandidateRepository.findDistinctUnblockedStudentIdsByAssessmentId(assessmentId);
    }

    @Override
    public List<AssessmentCandidate> findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID assessmentId) {
        return springDataAssessmentCandidateRepository.findByAssessmentIdAndScheduleIdIsNullOrderByAssignedAtAsc(assessmentId).stream()
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
    public List<AssessmentCandidate> findByIdInAndAssessmentId(Collection<UUID> ids, UUID assessmentId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentCandidateRepository.findByIdInAndAssessmentId(ids, assessmentId).stream()
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
    public boolean existsByAssessmentIdAndScheduleIdIsNotNull(UUID assessmentId) {
        return springDataAssessmentCandidateRepository.existsByAssessmentIdAndScheduleIdIsNotNull(assessmentId);
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
        // AssessmentScheduleRoomValidator bỏ qua khi thiếu dữ liệu.
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

