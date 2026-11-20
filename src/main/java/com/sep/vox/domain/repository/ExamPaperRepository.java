package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;

public interface ExamPaperRepository {
    ExamPaper save(ExamPaper paper);
    List<ExamPaper> saveAll(Collection<ExamPaper> papers);
    Optional<ExamPaper> findById(UUID id);
    List<ExamPaper> findByIdIn(Collection<UUID> ids);
    List<ExamPaper> findByExamId(UUID examId);
    List<ExamPaper> findByExamIdIn(Collection<UUID> examIds);
    List<ExamPaper> findByExamIdAndStatus(UUID examId, ExamPaperStatus status);
    boolean existsByExamId(UUID examId);
    int nextVariant(UUID examId);
    void deleteById(UUID id);
    void deleteByExamId(UUID examId);
}
