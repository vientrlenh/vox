package com.sep.vox.domain.valueobject.rubriccriterionbandexample;

import java.math.BigDecimal;

public record RubricCriterionBandExample(
    String transcript,
    String explanation,
    BigDecimal expectedScore
) {
    public RubricCriterionBandExample {
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException("Bảng điểm ví dụ không được để trống");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Giải thích của ví dụ không được để trống");
        }
        if (expectedScore == null || expectedScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Điểm số dự kiến không được để trống hoặc dưới 0");
        }
    }
}
