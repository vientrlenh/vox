package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;

public interface ExamGradingAssignmentRepository {
    Optional<ExamGradingAssignment> findById(UUID id);

    /** Một bài chỉ có một phân công — DB enforce bằng unique index trên candidate_result_id. */
    Optional<ExamGradingAssignment> findByCandidateResultId(UUID candidateResultId);

    boolean existsByCandidateResultId(UUID candidateResultId);

    List<ExamGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    ExamGradingAssignment save(ExamGradingAssignment assignment);

    List<ExamGradingAssignment> saveAll(List<ExamGradingAssignment> assignments);

    void deleteById(UUID id);
}
