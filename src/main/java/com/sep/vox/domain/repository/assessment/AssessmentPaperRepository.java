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
    List<AssessmentPaper> findByAssessmentId(UUID assessmentId);
    List<AssessmentPaper> findByAssessmentIdIn(Collection<UUID> assessmentIds);
    List<AssessmentPaper> findByAssessmentIdAndStatus(UUID assessmentId, AssessmentPaperStatus status);
    boolean existsByAssessmentId(UUID assessmentId);
    int nextVariant(UUID assessmentId);
    void deleteById(UUID id);
    void deleteByAssessmentId(UUID assessmentId);
}
