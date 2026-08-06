package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.util.Map;

public record WeaknessVectorSettings(
    double alpha,
    Map<String, Double> shrinkageByCriterion,
    int reliableObservationCount,
    int minimumCriteriaPerEvaluation,
    /**
     * Số học sinh tối thiểu để một cohort (lớp HOẶC khối) được dùng làm prior. Áp cho CẢ HAI
     * tầng: một tập gồm đúng một người thì trung bình của tập chính là người đó, nên co Bayes
     * co về chính mình và không co gì cả.
     */
    int minimumCohortStudents,
    Duration observationWindow,
    int minimumSubAttributeFrequency,
    Duration staleAfter,
    int batchSize
) {

    public double shrinkageFor(String criterionCode) {
        return shrinkageByCriterion.getOrDefault(criterionCode.toUpperCase(), 5.0);
    }
}
