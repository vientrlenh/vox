package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamPaperItem;

public interface ExamPaperItemRepository {
    ExamPaperItem save(ExamPaperItem item);
    Optional<ExamPaperItem> findById(UUID id);
    List<ExamPaperItem> findBySectionId(UUID sectionId);
    List<ExamPaperItem> findBySectionIdIn(Collection<UUID> sectionIds);
    List<ExamPaperItem> findByPaperId(UUID paperId);
    /** Nạp item của nhiều mã đề trong một query — dùng khi tính lại thời lượng cho cả kỳ thi. */
    List<ExamPaperItem> findByPaperIdIn(Collection<UUID> paperIds);
    boolean existsUnassignedItemByPaperId(UUID paperId);
    void deleteById(UUID id);
}
