package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;

public final class ScoreRangeValidator {

    private ScoreRangeValidator() {}

    /**
     * Dùng chung cho RubricResultBand và RubricCriterion: khoảng điểm của item phải nằm trọn trong
     * thang điểm tổng (scoringScaleMin/Max) của RubricVersion cha.
     */
    public static void assertWithinScale(BigDecimal scaleMin, BigDecimal scaleMax, BigDecimal rangeMin,
            BigDecimal rangeMax, String itemName) {
        if (rangeMin.compareTo(scaleMin) < 0 || rangeMax.compareTo(scaleMax) > 0) {
            throw new IllegalArgumentException(
                    "'" + itemName + "' (" + rangeMin + " - " + rangeMax
                            + ") phải nằm trong khoảng thang điểm tổng của Rubric Version (" + scaleMin + " - "
                            + scaleMax + ").");
        }
    }
}
