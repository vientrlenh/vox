package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamResultAppeal;

public interface ExamResultAppealRepository {
    Optional<ExamResultAppeal> findById(UUID id);
    ExamResultAppeal save(ExamResultAppeal appeal);
    boolean existsOpenByCandidateResultId(UUID candidateResultId);
    List<ExamResultAppeal> findByCandidateResultId(UUID candidateResultId);
    void deleteByIdIn(Collection<UUID> ids);
}
