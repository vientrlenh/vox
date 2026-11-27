package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

public interface InterestQuizItemRepository {

    /** The question set generated specifically for this student, if it exists -- see hasQuizItemsForStudent. */
    /** The whole pool (soft limit) so the balanced selector has enough candidates -- see InterestQuizItemSelector. */
    List<InterestQuizSeedItem> findAllActiveQuizItems();

    List<InterestQuizSeedItem> findAllActiveQuizItemsForStudent(UUID studentId);

    boolean hasQuizItemsForStudent(UUID studentId);

    Optional<InterestQuizSeedItem> findActiveQuizItem(UUID itemId);

    void seedQuizItemsIfEmpty(List<InterestQuizSeedItem> items);

    /** Saves the freshly generated (LLM) set for this student -- active right away, without review. */
    void saveGeneratedForStudent(UUID studentId, List<InterestQuizSeedItem> items);

    int deactivateGeneratedForStudent(UUID studentId);
}
