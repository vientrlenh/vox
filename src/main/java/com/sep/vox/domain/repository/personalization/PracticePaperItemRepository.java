package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticePaperItem;

public interface PracticePaperItemRepository {

    void save(PracticePaperItem item);

    // Tổng preparation+response+followup giây của mọi câu đã gắn vào paper -- dùng để tính
    // ngân sách phiên đã dùng (mục 1/2.4 bước 3), không cần cột riêng trên PracticeSession
    int sumPlannedSecondsForPaper(UUID paperId);

    // Id các câu đã chọn trong paper, theo đúng thứ tự slot -- dùng làm alreadyChosenInSession
    List<UUID> findQuestionIdsForPaper(UUID paperId);

    int countItemsForPaper(UUID paperId);
}
