package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * The three columns that the NAME-based duplicate check of {@code TopicSuggestionService} needs, and
 * only those three.
 *
 * <p>It exists to replace loading the full {@code PracticeTopicJpaEntity}: the comparison is
 * {@code normalize(name)} equality or {@code tokenSimilarity(name) >= 0.90}, so the other 8 columns of
 * the entity -- including the TEXT {@code description} -- are never read.
 */
public interface TopicNameCardInfo {

    UUID getId();

    String getName();

    String getInterestDimension();
}
