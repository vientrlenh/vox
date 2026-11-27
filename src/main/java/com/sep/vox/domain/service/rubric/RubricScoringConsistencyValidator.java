package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * Guard at PUBLISH time: the set of criteria must match the way weights are declared, otherwise the question score would leave
 * the rubric scale.
 *
 * <p>Extracted to be shared 2026-08-14. Before that this check lived as a private method in
 * {@code ChangeSchoolRubricVersionStatusUseCase}, while
 * {@code ChangeSystemRubricVersionStatusUseCase} checked NOTHING -- meaning a system admin could publish
 * a version whose question score shoots outside the scale. Placed in a domain service so both publish paths
 * call one shared version, instead of a second copy that drifts apart.
 *
 * <h2>Why check at publish time</h2>
 *
 * <p>Every add/edit/delete of a criterion and every edit of the scale is only allowed while the version is DRAFT.
 * So PUBLISH is the only gate through which a configuration goes from "being drafted" to "used for real grading" -- blocking here
 * is airtight, and there is no need to repeat the check at each individual edit.
 *
 * <h2>Two ways to declare weights</h2>
 *
 * <p>Both grade each criterion on the WHOLE scale and then combine with the same formula
 * {@code Σ(score × weight) / Σweight} (see {@code RubricItemScoreFormula}). They differ in the
 * total-weight constraint, and that is a difference in HOW THEY ARE DECLARED, not in the calculation:
 *
 * <ul>
 *   <li>{@code SUM} -- allocation. A weight is the criterion's share of the total, adding up to exactly 1
 *       (100%). Suitable when criteria differ in importance: "pronunciation counts for 30% of the score".
 *   <li>{@code WEIGHTED_AVERAGE} -- average. Every criterion is balanced, each with weight 1 (100%),
 *       so the total equals exactly the number of criteria. Combined, it is simply the arithmetic mean.
 * </ul>
 *
 * <p>Both constraints guarantee the same thing: the scale ceiling is always reachable. A student who achieves the maximum on
 * every criterion gets {@code Σ(max × w) / Σw = max}, however the weights are split.
 *
 * <p>A bug really encountered before the guard existed: 5 criteria each worth at most 10 points on a 0-10 scale,
 * added directly gave 38.4 and was then clamped to 10 -- every result got 10/10.
 */
public final class RubricScoringConsistencyValidator {

    private RubricScoringConsistencyValidator() {
    }

    /**
     * @param criteria every criterion of the version; an empty list is rejected because a version
     *                 without criteria cannot be used for grading
     */
    public static void assertPublishable(
            RubricTotalScoreMethod method,
            BigDecimal scaleMin,
            BigDecimal scaleMax,
            List<RubricCriterion> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot publish: this version has no criteria yet.");
        }

        assertEveryCriterionCoversScale(scaleMin, scaleMax, criteria);

        BigDecimal weightSum = criteria.stream()
                .map(criterion -> criterion.getWeight())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        if (method == RubricTotalScoreMethod.WEIGHTED_AVERAGE) {
            assertWeightsEqualCriteriaCount(criteria.size(), weightSum);
            return;
        }
        assertWeightsSumToOne(criteria.size(), weightSum);
    }

    private static void assertWeightsEqualCriteriaCount(int criteriaCount, BigDecimal weightSum) {
        // Average: every criterion is balanced at 100% -> the total equals the number of criteria.
        //
        // Using "equals the number of criteria" instead of "equals 1" is deliberate: the weight
        // column is numeric(6,2), so splitting evenly against 1 gives 0.33+0.33+0.33 = 0.99 for 3
        // criteria, which never matches and forces a skewed 0.34/0.33/0.33. Against n all three
        // are 1.00, which matches exactly for any number of criteria, and adding a criterion does
        // not require rewriting the existing ones.
        BigDecimal expected = BigDecimal.valueOf(criteriaCount);
        if (weightSum.compareTo(expected) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Cannot publish: the Average method requires every criterion to be balanced at"
                            + " weight 100%%, so the total weight of %d criteria must equal %s."
                            + " It is currently %s.",
                    criteriaCount, expected.toPlainString(), weightSum.toPlainString()));
        }
    }

    private static void assertWeightsSumToOne(int criteriaCount, BigDecimal weightSum) {
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Cannot publish: the Allocation method requires the total weight of %d criteria"
                            + " to equal 100%% (i.e. 1), it is currently %s (%s%%).",
                    criteriaCount, weightSum.toPlainString(),
                    weightSum.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()));
        }
    }

    /**
     * Every criterion must cover the scale EXACTLY. The weighted average of values in [a,b] lies
     * in [a,b] -- but only when EVERY criterion lies in that same [a,b]. A mismatch at either end
     * breaks it in two ways:
     *
     * <ul>
     *   <li>a criterion WIDER than the scale (scale 4-10, criterion 0-100): the score shoots up to
     *       100 and is clamped to 10, so every decent answer becomes 10.
     *   <li>a criterion NARROWER than the scale (scale 4-10, one criterion 4-5): a student who does
     *       everything perfectly can never reach 10, so the scale ceiling becomes a dead ceiling.
     * </ul>
     *
     * <p>Differences in importance between criteria are expressed by WEIGHT, not by score range.
     */
    private static void assertEveryCriterionCoversScale(
            BigDecimal scaleMin, BigDecimal scaleMax, List<RubricCriterion> criteria) {
        RubricCriterion mismatched = criteria.stream()
                .filter(criterion -> criterion.getMinScore() == null
                        || criterion.getMaxScore() == null
                        || criterion.getMinScore().compareTo(scaleMin) != 0
                        || criterion.getMaxScore().compareTo(scaleMax) != 0)
                .findFirst()
                .orElse(null);
        if (mismatched == null) {
            return;
        }
        throw new IllegalArgumentException(String.format(
                "Cannot publish: criterion \"%s\" has score range %s - %s, while the scale of the"
                        + " version is %s - %s. Each criterion must cover exactly the scale; differing"
                        + " importance between criteria is expressed with WEIGHT, not with"
                        + " the score range.",
                mismatched.getName(),
                mismatched.getMinScore() == null ? "?" : mismatched.getMinScore().toPlainString(),
                mismatched.getMaxScore() == null ? "?" : mismatched.getMaxScore().toPlainString(),
                scaleMin.toPlainString(),
                scaleMax.toPlainString()));
    }
}
