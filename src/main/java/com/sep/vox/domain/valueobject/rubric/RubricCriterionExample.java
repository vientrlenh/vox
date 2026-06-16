package com.sep.vox.domain.valueobject.rubric;

import java.math.BigDecimal;

public record RubricCriterionExample(
    String transcript,
    String explanation,
    BigDecimal expectedScore
) {
    public RubricCriterionExample {
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException("Ví dụ của tiêu chí không được để trống");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Giải thích cho ví dụ của tiêu chí không được để trống");
        }
        if (expectedScore == null || expectedScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm số dự kiến không được để trống hoặc dưới 0");
        }
    }
}
