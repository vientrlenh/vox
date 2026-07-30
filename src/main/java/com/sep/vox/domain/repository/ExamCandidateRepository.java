package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidate;

public interface ExamCandidateRepository {
    ExamCandidate save(ExamCandidate candidate);
    List<ExamCandidate> saveAll(Collection<ExamCandidate> candidates);
    List<ExamCandidate> findByExamId(UUID examId);
    List<ExamCandidate> findByExamIdIn(Collection<UUID> examIds);
    long countByExamId(UUID examId);
    Map<UUID, Long> countByExamIdIn(Collection<UUID> examIds);

    Optional<ExamCandidate> findById(UUID id);
    Optional<ExamCandidate> findByExamIdAndStudentId(UUID examId, UUID studentId);
    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);
    List<ExamCandidate> findByStudentId(UUID studentId);
    List<ExamCandidate> findByScheduleId(UUID scheduleId);
    Set<UUID> findStudentIdsByExamId(UUID examId);
    List<ExamCandidate> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId);
    List<ExamCandidate> findByIdInAndExamId(Collection<UUID> ids, UUID examId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId);
    Optional<ExamCandidate> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    List<ExamCandidate> findActiveCandidates(UUID studentId, Instant now);
}

