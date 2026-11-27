package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;

public final class ScoreRangeValidator {

    private ScoreRangeValidator() {}

    /**
     * Shared by RubricResultBand and RubricCriterion: the score range of the item must lie entirely within
     * the overall score scale (scoringScaleMin/Max) of the parent RubricVersion.
     */
    public static void assertWithinScale(BigDecimal scaleMin, BigDecimal scaleMax, BigDecimal rangeMin,
            BigDecimal rangeMax, String itemName) {
        if (rangeMin.compareTo(scaleMin) < 0 || rangeMax.compareTo(scaleMax) > 0) {
            throw new IllegalArgumentException(
                    "'" + itemName + "' (" + rangeMin + " - " + rangeMax
                            + ") must lie within the overall score scale of the Rubric Version (" + scaleMin + " - "
                            + scaleMax + ").");
        }
    }
}
