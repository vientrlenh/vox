package com.sep.vox.domain.service.rubric;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class ScoreRangeValidatorTests {

    @Test
    void rangeWithinScale_passes() {
        assertDoesNotThrow(() -> ScoreRangeValidator.assertWithinScale(
                BigDecimal.valueOf(0), BigDecimal.valueOf(10), BigDecimal.valueOf(4), BigDecimal.valueOf(6), "Trung bình"));
    }

    @Test
    void rangeExactlyAtScaleBounds_passes() {
        assertDoesNotThrow(() -> ScoreRangeValidator.assertWithinScale(
                BigDecimal.valueOf(0), BigDecimal.valueOf(10), BigDecimal.valueOf(0), BigDecimal.valueOf(10), "Toàn thang"));
    }

    @Test
    void rangeBelowScaleMin_throws() {
        assertThrows(IllegalArgumentException.class, () -> ScoreRangeValidator.assertWithinScale(
                BigDecimal.valueOf(0), BigDecimal.valueOf(10), BigDecimal.valueOf(-1), BigDecimal.valueOf(5), "Yếu"));
    }

    @Test
    void rangeAboveScaleMax_throws() {
        assertThrows(IllegalArgumentException.class, () -> ScoreRangeValidator.assertWithinScale(
                BigDecimal.valueOf(0), BigDecimal.valueOf(10), BigDecimal.valueOf(8), BigDecimal.valueOf(11), "Giỏi"));
    }
}
