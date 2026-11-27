package com.sep.vox.domain.service.assessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * <p>The separate {@code SUM} branch was removed 2026-08-14. Before that {@code SUM} meant "each criterion takes
 * a slice of the scale (10 = 2+2+2+2+2), added directly without weights" -- and with that declaration style the weight
 * was dead data: it took no part in the calculation and was not checked at publish time, so the number shown on the
 * UI could be completely made up. Now both methods grade each criterion on the WHOLE scale and
 * combine with the same expression below; they differ only in the total-weight constraint at publish time (see
 * {@code RubricScoringConsistencyValidator}), i.e. they differ in HOW THEY ARE DECLARED, not in the calculation.
 *
 * <p>The expression {@code Σ(score × weight) / Σweight} is invariant under scaling all weights proportionally,
 * so it is correct for both constraints: allocation ({@code Σw = 1}) divides by 1 and reduces to the weighted
 * sum, while average ({@code Σw = n}) divides by n and reduces to the arithmetic mean.
 */
public final class RubricItemScoreFormula {

    private RubricItemScoreFormula() {
    }

    /**
     * A RESOLVED criterion: the score already clamped to the range of that criterion itself, together with the weight
     * declared in the rubric.
     *
     * @param weight null is treated as no weight declared -- the {@code rubric_criterions.weight} column is
     *               NOT NULL so in practice it does not happen, but the formula should not depend
     *               on the constraint of one specific table.
     */
    public record ScoredCriterion(BigDecimal score, BigDecimal weight) {
    }

    /**
     * @param scored   the resolved criteria; if empty, returns {@code scaleMin}
     * @param scaleMin the score scale of the rubric version -- the result is always clamped into the scale
     */
    public static BigDecimal compute(
            List<ScoredCriterion> scored,
            BigDecimal scaleMin,
            BigDecimal scaleMax) {
        if (scored == null || scored.isEmpty()) {
            return clampToScale(scaleMin == null ? BigDecimal.ZERO : scaleMin, scaleMin, scaleMax);
        }

        return clampToScale(weightedOrPlainAverage(scored), scaleMin, scaleMax);
    }

    private static BigDecimal weightedOrPlainAverage(List<ScoredCriterion> scored) {
        BigDecimal plainSum = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (ScoredCriterion item : scored) {
            if (item == null || item.score() == null) {
                continue;
            }
            plainSum = plainSum.add(item.score());
            if (item.weight() != null) {
                weightedSum = weightedSum.add(item.score().multiply(item.weight()));
                weightSum = weightSum.add(item.weight());
            }
        }
        if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
            return weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
        }
        // No criterion declares a weight -> plain average. Safety net for legacy data that
        // predates the publish-time constraint; it still yields a score inside the scale.
        return plainSum.divide(BigDecimal.valueOf(scored.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal clampToScale(BigDecimal score, BigDecimal scaleMin, BigDecimal scaleMax) {
        BigDecimal safe = score == null ? BigDecimal.ZERO : score;
        if (scaleMin != null && safe.compareTo(scaleMin) < 0) {
            safe = scaleMin;
        }
        if (scaleMax != null && safe.compareTo(scaleMax) > 0) {
            safe = scaleMax;
        }
        return safe.setScale(2, RoundingMode.HALF_UP);
    }
}
