package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidate;

public interface ExamCandidateRepository {
    ExamCandidate save(ExamCandidate candidate);
    List<ExamCandidate> saveAll(Collection<ExamCandidate> candidates);
    List<ExamCandidate> findByExamId(UUID examId);
    long countByScheduleId(UUID scheduleId);
}
