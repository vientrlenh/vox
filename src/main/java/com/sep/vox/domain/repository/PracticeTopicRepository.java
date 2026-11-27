package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeTopic;

public interface PracticeTopicRepository {

    Optional<PracticeTopic> findTopicById(UUID id);

    boolean existsActiveById(UUID id);

    PracticeTopic save(PracticeTopic topic);

    /** topicId -> interestDimension, for every active topic. Used to convert topic events into per-dimension scores. */
    /**
     * The interest dimension of EXACTLY the topics asked about, keyed by id.
     *
     * <p>Replaces the old {@code findAllTopicDimensions()} (removed 2026-08-11) which called plain
     * {@code findAll()}: loading EVERY topic, INCLUDING disabled ones, as full entities (including the TEXT column {@code description})
     * just to build a two-field map -- and it ran after EVERY practice session.
     *
     * <p>Deliberately does NOT filter on {@code active}: the old version did not filter either, and interest events attached to a
     * disabled topic must still be counted. Adding a filter here would silently change the dimension score result.
     *
     * @param topicIds empty returns an empty map -- must not be read as "take all"
     */
    Map<UUID, String> findDimensionsByIds(java.util.Collection<UUID> topicIds);

    /** Size of the topic pool feeding the greeting batch (minus topics materialized from the school's question bank). */
    long countOfferablePool();

    // REMOVED 2026-08-11: findAllActiveOrderByName(). Its only user was TopicSuggestionService
    // .topicNames(), which sent the whole name list into the LLM prompt -- also removed in the same change.

    Optional<PracticeTopic> findByNormalizedName(String normalizedName);

    List<String> findExhaustedTopicNames(UUID studentId);

    /** dimension -> current interest score of the student, per the latest profile. */
    Map<String, Double> findInterestScoresByDimension(UUID studentId);

    Optional<PracticeTopic> findBySourceQuestionTopicId(UUID sourceQuestionTopicId);
}
