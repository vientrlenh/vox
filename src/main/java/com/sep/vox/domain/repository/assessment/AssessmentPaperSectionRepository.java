package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentPaperSection;

public interface AssessmentPaperSectionRepository {
    AssessmentPaperSection save(AssessmentPaperSection section);
    Optional<AssessmentPaperSection> findById(UUID id);
    List<AssessmentPaperSection> findByPaperId(UUID paperId);
    List<AssessmentPaperSection> findByPaperIdIn(Collection<UUID> paperIds);
    void deleteById(UUID id);
    void deleteByPaperIdIn(Collection<UUID> paperIds);
}
