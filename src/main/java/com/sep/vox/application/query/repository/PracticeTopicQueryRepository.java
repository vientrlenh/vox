package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicNameCardInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;

public interface PracticeTopicQueryRepository {

    List<RankedTopicInfo> findRankedTopics(UUID studentId, String goal);

    List<TopicSearchRowInfo> searchTopics(UUID studentId, String pattern, String normalized);

    /** Reloads active topics by id -- hydrates the result of a vector search from Postgres. */
    List<TopicSearchRowInfo> findActiveByIds(UUID studentId, java.util.Collection<UUID> topicIds);

    Optional<TopicSearchRowInfo> findRandomActiveTopic(UUID studentId);

    List<TopicSearchRowInfo> findSavedTopics(UUID studentId);

    /**
     * A card (id, name, dimension) of each active topic -- enables name-based duplicate detection.
     *
     * <p>Replaces the old {@code findAllActive()} (removed 2026-08-11), which returned full entities with the
     * TEXT {@code description} column, while the caller only reads these three fields.
     */
    List<TopicNameCardInfo> findActiveNameCards();

    /**
     * PUBLISHED topics in the question bank (question_bank/question_topic) of exactly the student's
     * current school + grade -- the topic source for ASSESSMENT_PREP practice. A bank not
     * attached to any grade applies to every grade in that school.
     */
    List<QuestionTopicInfo> findPublishedAssessmentTopics(UUID schoolId, UUID gradeId);
}
