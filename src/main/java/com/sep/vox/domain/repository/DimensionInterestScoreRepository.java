package com.sep.vox.domain.repository;

import java.util.Map;
import java.util.UUID;

public interface DimensionInterestScoreRepository {

    void primeBaselineFromScoreWhereMissing(UUID learnerProfileId);

    /** Current score per dimension — baseline first if present, otherwise score. */
    Map<String, Double> findByLearnerProfile(UUID learnerProfileId);

    void upsertScore(UUID learnerProfileId, String dimension, double score);

    void replaceScores(UUID learnerProfileId, Map<String, Double> scores);
}
