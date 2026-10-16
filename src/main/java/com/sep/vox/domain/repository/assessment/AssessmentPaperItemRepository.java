package com.sep.vox.domain.repository.assessment;

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
    /** Nạp item của nhiều mã đề trong một query — dùng khi tính lại thời lượng cho cả kỳ thi. */
    List<AssessmentPaperItem> findByPaperIdIn(Collection<UUID> paperIds);
    boolean existsUnassignedItemByPaperId(UUID paperId);
    void deleteById(UUID id);
    void deleteByPaperIdIn(Collection<UUID> paperIds);
}
