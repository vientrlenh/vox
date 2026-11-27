package com.sep.vox.application.port.input.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.InterestDimensionRepository;

/**
 * Normalizes the raw interest-quiz score into a [0,1] vector per dimension.
 *
 * The dimension list is read from {@link InterestDimensionRepository} and is no longer a hard-coded constant:
 * when SYSTEM_ADMIN adds a new dimension the system picks it up immediately, with no redeploy (see
 * V15__personalize.sql, section 19. interest_dimensions, for why the hard-coded constant had to go).
 *
 * Scored ipsatively (compared within the student themselves, the true nature of forced-choice):
 * min-max scaling so there is ALWAYS one dimension = 0.0 and one dimension = 1.0. It answers "among these dimensions where does the student
 * lean", not "how strongly do they like it". When all dimensions are equal it returns 0.5
 * for everything instead of dividing by 0.
 */
@Service
@RequiredArgsConstructor
public class InterestQuizScorer {

    private final InterestDimensionRepository dimensionRepository;

    /** The dimensions that are put to the student in the quiz -- excluding system dimensions such as ACADEMIC_ASSESSMENT. */
    public List<String> quizDimensionCodes() {
        return dimensionRepository.findQuizEligible().stream()
            .map(dimension -> dimension.getCode())
            .toList();
    }

    public Map<String, Double> normalize(Map<String, Integer> rawScores) {
        List<String> dimensions = quizDimensionCodes();
        if (dimensions.isEmpty()) {
            return Map.of();
        }
        int min = dimensions.stream()
            .mapToInt(dimension -> rawScores.getOrDefault(dimension, 0))
            .min()
            .orElse(0);
        int max = dimensions.stream()
            .mapToInt(dimension -> rawScores.getOrDefault(dimension, 0))
            .max()
            .orElse(0);
        Map<String, Double> result = new LinkedHashMap<String, Double>();
        for (String dimension : dimensions) {
            Integer raw = rawScores.getOrDefault(dimension, 0);
            result.put(dimension, min == max ? 0.5 : (double) (raw - min) / (max - min));
        }
        return result;
    }
}
