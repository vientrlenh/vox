package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateResult;

public interface ExamCandidateResultRepository {
    Optional<ExamCandidateResult> findById(UUID id);
    Optional<ExamCandidateResult> findBySessionId(UUID sessionId);
    List<ExamCandidateResult> findBySessionIdIn(Collection<UUID> sessionIds);
    ExamCandidateResult save(ExamCandidateResult result);
    void deleteBySessionId(UUID sessionId);
}
