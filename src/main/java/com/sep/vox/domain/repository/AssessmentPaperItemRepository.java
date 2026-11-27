package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;

public interface AssessmentPaperItemRepository {
    AssessmentPaperItem save(AssessmentPaperItem item);
    Optional<AssessmentPaperItem> findById(UUID id);
    List<AssessmentPaperItem> findBySectionId(UUID sectionId);
    List<AssessmentPaperItem> findBySectionIdIn(Collection<UUID> sectionIds);
    List<AssessmentPaperItem> findByPaperId(UUID paperId);
    /** Loads the items of several papers in one query — used when recalculating the duration for a whole assessment. */
    List<AssessmentPaperItem> findByPaperIdIn(Collection<UUID> paperIds);
    boolean existsUnassignedItemByPaperId(UUID paperId);
    void deleteById(UUID id);
    void deleteByPaperIdIn(Collection<UUID> paperIds);
}
