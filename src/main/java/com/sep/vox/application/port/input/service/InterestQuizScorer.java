package com.sep.vox.application.port.input.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InterestQuizScorer {

    public static final List<String> DIMENSIONS = List.of(
        "ENTERTAINMENT_MEDIA",
        "TECH_GAMING",
        "SPORTS_HEALTH",
        "PEOPLE_SOCIETY",
        "TRAVEL_PLACES",
        "FUTURE_SCIENCE"
    );

    private InterestQuizScorer() {
    }

    public static Map<String, Double> normalize(Map<String, Integer> rawScores) {
        var min = DIMENSIONS.stream().mapToInt(dimension -> rawScores.getOrDefault(dimension, 0)).min().orElse(0);
        var max = DIMENSIONS.stream().mapToInt(dimension -> rawScores.getOrDefault(dimension, 0)).max().orElse(0);
        var result = new LinkedHashMap<String, Double>();
        for (var dimension : DIMENSIONS) {
            var raw = rawScores.getOrDefault(dimension, 0);
            result.put(dimension, min == max ? 0.5 : (double) (raw - min) / (max - min));
        }
        return result;
    }
}
