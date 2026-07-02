package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;

public interface ExamPaperRepository {
    ExamPaper save(ExamPaper paper);
    Optional<ExamPaper> findById(UUID id);
    List<ExamPaper> findByExamId(UUID examId);
    List<ExamPaper> findByExamIdAndStatus(UUID examId, ExamPaperStatus status);
    boolean existsByExamId(UUID examId);
    int nextVariant(UUID examId);
    void deleteById(UUID id);
}
