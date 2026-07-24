package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSecurePool;

public interface ExamSecurePoolRepository {
    ExamSecurePool save(ExamSecurePool pool);
    Optional<ExamSecurePool> findById(UUID id);
    Optional<ExamSecurePool> findByExamId(UUID examId);
    List<ExamSecurePool> findByExamIdIn(Collection<UUID> examIds);
}
