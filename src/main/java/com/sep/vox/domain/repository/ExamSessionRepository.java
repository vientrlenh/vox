package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSession;

public interface ExamSessionRepository {
    Optional<ExamSession> findById(UUID id);
    boolean existsById(UUID id);
    ExamSession save(ExamSession session);
}
