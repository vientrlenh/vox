package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

public interface InterestQuizItemRepository {

    /** Bộ câu sinh riêng cho đúng học sinh này, nếu đã có -- xem hasQuizItemsForStudent. */
    /** Cả kho (giới hạn mềm) để bộ chọn cân bằng có đủ ứng viên -- xem InterestQuizItemSelector. */
    List<InterestQuizSeedItem> findAllActiveQuizItems();

    List<InterestQuizSeedItem> findAllActiveQuizItemsForStudent(UUID studentId);

    boolean hasQuizItemsForStudent(UUID studentId);

    Optional<InterestQuizSeedItem> findActiveQuizItem(UUID itemId);

    void seedQuizItemsIfEmpty(List<InterestQuizSeedItem> items);

    /** Lưu bộ câu vừa sinh (LLM) riêng cho học sinh này -- active ngay, không qua duyệt. */
    void saveGeneratedForStudent(UUID studentId, List<InterestQuizSeedItem> items);
}
