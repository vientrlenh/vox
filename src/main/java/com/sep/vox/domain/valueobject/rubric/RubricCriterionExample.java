package com.sep.vox.domain.valueobject.rubric;

import java.math.BigDecimal;

public record RubricCriterionExample(
    String transcript,
    String explanation,
    BigDecimal expectedScore
) {
    public RubricCriterionExample {
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException("The criterion example must not be blank");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("The explanation of the criterion example must not be blank");
        }
        if (expectedScore == null || expectedScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("The expected score must not be blank or below 0");
        }
    }
}
