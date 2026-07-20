package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSession;

public interface ExamSessionRepository {
    Optional<ExamSession> findByIdAndInProgress(UUID id);
    ExamSession save(ExamSession session);
    Optional<ExamSession> findActiveByExamIdAndCandidateId(UUID examId, UUID candidateId);
}
