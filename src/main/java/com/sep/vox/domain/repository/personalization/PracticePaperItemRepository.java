package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticePaperItem;

public interface PracticePaperItemRepository {

    void save(PracticePaperItem item);

    List<UUID> findQuestionIdsForPaper(UUID paperId);

    int deleteLastItemForPaper(UUID paperId, UUID questionId);
}
