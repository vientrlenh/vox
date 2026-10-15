package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;

public interface AssessmentPaperRepository {
    AssessmentPaper save(AssessmentPaper paper);
    List<AssessmentPaper> saveAll(Collection<AssessmentPaper> papers);
    Optional<AssessmentPaper> findById(UUID id);
    List<AssessmentPaper> findByIdIn(Collection<UUID> ids);
    List<AssessmentPaper> findByExamId(UUID examId);
    List<AssessmentPaper> findByExamIdIn(Collection<UUID> examIds);
    List<AssessmentPaper> findByExamIdAndStatus(UUID examId, AssessmentPaperStatus status);
    boolean existsByExamId(UUID examId);
    int nextVariant(UUID examId);
    void deleteById(UUID id);
    void deleteByExamId(UUID examId);
}
