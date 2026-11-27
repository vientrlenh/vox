package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.personalization.TopicSuggestionJpaEntity;

/**
 * Only the inherited findById/save from JpaRepository remain.
 *
 * <p>REMOVED 2026-08-07: {@code countWeeklyKeywordRequests} -- the query counting keyword typing requests in
 * the calendar week, which fed the 3-requests/week quota that was dropped in {@code TopicSuggestionService.generateFromKeyword}.
 * Nobody calls it any more. The {@code topic_suggestions} table is kept as is and is now purely a log.
 */
public interface SpringDataTopicSuggestionRepository
        extends JpaRepository<TopicSuggestionJpaEntity, UUID> {

}
