package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionStatusCountInfo;

/**
 * The read side for questions. Kept apart from {@code QuestionRepository} (write side) because the queries
 * here return shapes that serve screens and not aggregates.
 */
public interface QuestionQueryRepository {

    /**
     * Counts questions by status, applying EXACTLY the access conditions of
     * {@code SpringDataQuestionRepository.findAccessible}. The two must match absolutely:
     * when a user clicks a number and filters the list by that status, they must get exactly that many rows.
     *
     * <p>There is no {@code status} parameter: it is the very dimension being grouped.
     *
     * <p>Only returns statuses that ACTUALLY have questions -- {@code GROUP BY} produces no empty rows.
     * The caller fills in the full set of statuses itself if it needs a stable axis.
     *
     * @param topicNamePattern already in LIKE-pattern form (see StringNormalization.toLikePattern)
     * @param keywordPattern   already in LIKE-pattern form
     */
    List<QuestionStatusCountInfo> countAccessibleByStatus(
        UUID currentUserId,
        UUID currentSchoolId,
        boolean systemAdmin,
        boolean schoolAdmin,
        UUID questionBankId,
        UUID questionTopicId,
        String topicNamePattern,
        String type,
        String sharing,
        String scope,
        String keywordPattern
    );
}
