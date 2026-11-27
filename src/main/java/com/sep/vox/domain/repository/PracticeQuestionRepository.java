package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeQuestion;

public interface PracticeQuestionRepository {

    Optional<PracticeQuestion> findById(UUID id);

    
    List<PracticeQuestion> findUnseenByTopicAndCriterionAndRankRange(
        UUID topicId,
        UUID studentId,
        String criterion,
        String tense,
        int rankMin,
        int rankMax
    );

    List<PracticeQuestion> findByIds(List<UUID> ids);

    PracticeQuestion save(PracticeQuestion question);

    void saveGenerated(PracticeQuestion question);

    List<UUID> findPermanentlyExhaustedIds(UUID topicId, UUID studentId);

    void incrementUsageCount(UUID id);

    /** Gives back the usage when a question was chosen but the student never answered it. */
    void decrementUsageCount(UUID id);
}
