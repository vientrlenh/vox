package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

public interface InterestQuizItemRepository {

    List<InterestQuizSeedItem> findActiveQuizItems(int limit);

    /** Bộ câu sinh riêng cho đúng học sinh này, nếu đã có -- xem hasQuizItemsForStudent. */
    List<InterestQuizSeedItem> findActiveQuizItemsForStudent(UUID studentId, int limit);

    boolean hasQuizItemsForStudent(UUID studentId);

    Optional<InterestQuizSeedItem> findActiveQuizItem(UUID itemId);

    void seedQuizItemsIfEmpty(List<InterestQuizSeedItem> items);

    /** Lưu bộ câu vừa sinh (LLM) riêng cho học sinh này -- active ngay, không qua duyệt. */
    void saveGeneratedForStudent(UUID studentId, List<InterestQuizSeedItem> items);
}
