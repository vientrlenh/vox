package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.util.Map;

public record WeaknessVectorSettings(
    double alpha,
    Map<String, Double> shrinkageByCriterion,
    int reliableObservationCount,
    int minimumCriteriaPerEvaluation,
    int minimumClassPriorStudents,
    Duration observationWindow,
    Duration recentObservationWindow,
    int minimumSubAttributeFrequency,
    double frequencyWeight,
    double recentFrequencyWeight,
    Duration staleAfter,
    int batchSize
) {

    public double shrinkageFor(String criterionCode) {
        return shrinkageByCriterion.getOrDefault(criterionCode.toUpperCase(), 5.0);
    }
}
