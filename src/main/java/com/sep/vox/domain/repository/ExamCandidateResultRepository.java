package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidateResult;

public interface ExamCandidateResultRepository {
    Optional<ExamCandidateResult> findById(UUID id);
    List<ExamCandidateResult> findByIdIn(Collection<UUID> ids);
    Optional<ExamCandidateResult> findBySessionId(UUID sessionId);
    List<ExamCandidateResult> findBySessionIdIn(Collection<UUID> sessionIds);
    List<ExamCandidateResult> findByExamId(UUID examId);
    ExamCandidateResult save(ExamCandidateResult result);
    void deleteBySessionId(UUID sessionId);

    /** Xoá mềm kết quả của một phiên thi, cùng lúc với chính phiên đó. */
    int softDeleteBySessionId(UUID sessionId, Instant deletedAt, String reason);
}
