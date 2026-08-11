package com.sep.vox.domain.repository.personalization;

import java.util.Map;
import java.util.UUID;

public interface DimensionInterestScoreRepository {

    void primeBaselineFromScoreWhereMissing(UUID learnerProfileId);

    /** Điểm hiện tại theo dimension — ưu tiên baseline nếu có, không thì dùng score. */
    Map<String, Double> findByLearnerProfile(UUID learnerProfileId);

    void upsertScore(UUID learnerProfileId, String dimension, double score);

    void replaceScores(UUID learnerProfileId, Map<String, Double> scores);
}
