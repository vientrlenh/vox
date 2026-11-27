package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * The only two columns that {@code InterestVectorService.recomputeDimensionInterest} needs from
 * {@code practice_topics}: the id to match the interest event, and the dimension to add the score to.
 *
 * <p>It exists to replace loading the full {@code PracticeTopicJpaEntity} -- that entity has a TEXT
 * {@code description} and 9 other columns, none of which is used on this path.
 */
public interface TopicDimensionInfo {

    UUID getId();

    String getInterestDimension();
}
