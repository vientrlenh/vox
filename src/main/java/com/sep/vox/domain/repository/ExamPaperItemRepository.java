package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamPaperItem;

public interface ExamPaperItemRepository {
    ExamPaperItem save(ExamPaperItem item);
    Optional<ExamPaperItem> findById(UUID id);
    List<ExamPaperItem> findBySectionId(UUID sectionId);
    boolean existsUnassignedItemByPaperId(UUID paperId);
    void deleteById(UUID id);
}
