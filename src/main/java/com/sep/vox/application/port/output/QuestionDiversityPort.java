package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Measures the semantic overlap between candidate questions and already chosen questions, so that a
 * practice paper does not repeat ideas. Implemented by QuestionDiversityClient.
 */
public interface QuestionDiversityPort {

    /**
     * The highest similarity of each candidate question to the chosen group. When the AI service is
     * down it returns an EMPTY map (not a map of all zeros) -- the caller reads that as "could not
     * measure" and stops selecting more.
     */
    Map<UUID, Double> maxSimilarities(List<UUID> candidateIds, List<UUID> selectedIds);
}
