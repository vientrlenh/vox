package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamCandidateMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamCandidateRepository;

@Repository
public class ExamCandidateRepositoryImpl implements ExamCandidateRepository {

    private final SpringDataExamCandidateRepository springDataExamCandidateRepository;

    public ExamCandidateRepositoryImpl(SpringDataExamCandidateRepository springDataExamCandidateRepository) {
        this.springDataExamCandidateRepository = springDataExamCandidateRepository;
    }

    @Override
    public ExamCandidate save(ExamCandidate candidate) {
        try {
            var saved = springDataExamCandidateRepository.save(ExamCandidateMapper.toJpa(candidate));
            return ExamCandidateMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }
    }

    @Override
    public List<ExamCandidate> saveAll(Collection<ExamCandidate> candidates) {
        try {
            var entities = candidates.stream().map(ExamCandidateMapper::toJpa).toList();
            return springDataExamCandidateRepository.saveAll(entities).stream()
                .map(ExamCandidateMapper::toDomain)
                .toList();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }
    }

    @Override
    public List<ExamCandidate> findByExamId(UUID examId) {
        return springDataExamCandidateRepository.findByExamId(examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamCandidate> findByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return List.of();
        }
        return springDataExamCandidateRepository.findByExamIdIn(examIds).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public long countByExamId(UUID examId) {
        return springDataExamCandidateRepository.countByExamId(examId);
    }

    @Override
    public Map<UUID, Long> countByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return Map.of();
        }
        return springDataExamCandidateRepository.countByExamIdIn(examIds).stream()
            .collect(Collectors.toMap(
                SpringDataExamCandidateRepository.ExamIdCandidateCount::getExamId,
                SpringDataExamCandidateRepository.ExamIdCandidateCount::getCandidateCount
            ));
    }

    public Optional<ExamCandidate> findById(UUID id) {
        return springDataExamCandidateRepository.findById(id)
            .map(ExamCandidateMapper::toDomain);
    }

    @Override
    public Optional<ExamCandidate> findByExamIdAndStudentId(UUID examId, UUID studentId) {
        return springDataExamCandidateRepository.findByExamIdAndStudentId(examId, studentId)
            .map(ExamCandidateMapper::toDomain);
    }

    @Override
    public boolean existsByExamIdAndStudentId(UUID examId, UUID studentId) {
        return springDataExamCandidateRepository.existsByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public List<ExamCandidate> findByStudentId(UUID studentId) {
        return springDataExamCandidateRepository.findByStudentId(studentId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamCandidate> findByScheduleId(UUID scheduleId) {
        return springDataExamCandidateRepository.findByScheduleId(scheduleId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public Set<UUID> findStudentIdsByExamId(UUID examId) {
        return new HashSet<>(springDataExamCandidateRepository.findStudentIdsByExamId(examId));
    }

    @Override
    public List<ExamCandidate> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId) {
        return springDataExamCandidateRepository.findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamCandidate> findByIdInAndExamId(Collection<UUID> ids, UUID examId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataExamCandidateRepository.findByIdInAndExamId(ids, examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public long countByScheduleId(UUID scheduleId) {
        return springDataExamCandidateRepository.countByScheduleId(scheduleId);
    }

    @Override
    public boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId) {
        return springDataExamCandidateRepository.existsByScheduleIdAndStudentId(scheduleId, studentId);
    }

    @Override
    public boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId) {
        return springDataExamCandidateRepository.existsByExamIdAndScheduleIdIsNotNull(examId);
    }

    @Override
    public Optional<ExamCandidate> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId) {
        return springDataExamCandidateRepository.findByScheduleIdAndStudentId(scheduleId, studentId).map(ExamCandidateMapper::toDomain);
    }

    @Override
    public List<ExamCandidate> findActiveCandidates(UUID studentId, OffsetDateTime now) {
        return springDataExamCandidateRepository.findActiveCandidate(studentId, now)
            .stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }
}

