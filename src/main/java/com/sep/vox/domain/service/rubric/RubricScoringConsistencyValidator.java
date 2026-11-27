package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * Chốt chặn lúc PUBLISH: bộ tiêu chí phải khớp với cách khai trọng số, nếu không điểm câu sẽ rời
 * khỏi thang rubric.
 *
 * <p>Tách ra dùng chung 2026-08-14. Trước đó phép kiểm này nằm dưới dạng phương thức private trong
 * {@code ChangeSchoolRubricVersionStatusUseCase}, còn
 * {@code ChangeSystemRubricVersionStatusUseCase} thì KHÔNG kiểm gì -- nghĩa là system admin ban
 * hành được một phiên bản mà điểm câu vọt ra ngoài thang. Đặt ở domain service để hai đường publish
 * gọi chung một bản, không sinh ra bản chép thứ hai rồi trôi lệch.
 *
 * <h2>Vì sao kiểm ở lúc publish</h2>
 *
 * <p>Mọi thao tác thêm/sửa/xoá tiêu chí và sửa thang điểm đều chỉ cho phép khi version còn DRAFT.
 * Nên PUBLISH là cửa duy nhất một cấu hình đi từ "đang soạn" sang "dùng để chấm thật" -- chặn ở đây
 * là kín, và không cần kiểm lặp lại ở từng thao tác sửa lẻ.
 *
 * <h2>Hai kiểu khai trọng số</h2>
 *
 * <p>Cả hai đều chấm mỗi tiêu chí trên TOÀN thang rồi gộp bằng cùng một công thức
 * {@code Σ(điểm × trọng số) / Σtrọng số} (xem {@code RubricItemScoreFormula}). Khác nhau ở ràng
 * buộc tổng trọng số, và đó là khác biệt về CÁCH KHAI, không phải về phép tính:
 *
 * <ul>
 *   <li>{@code SUM} -- phân bổ. Trọng số là phần chia của tiêu chí trong tổng, cộng lại đúng 1
 *       (100%). Hợp khi các tiêu chí nặng nhẹ khác nhau: "phát âm chiếm 30% điểm".
 *   <li>{@code WEIGHTED_AVERAGE} -- trung bình. Mọi tiêu chí cân bằng, mỗi cái trọng số 1 (100%),
 *       nên tổng bằng đúng số tiêu chí. Gộp lại chính là trung bình cộng.
 * </ul>
 *
 * <p>Hai ràng buộc này đều bảo đảm cùng một điều: trần thang luôn với tới được. Học sinh đạt tối đa
 * mọi tiêu chí thì {@code Σ(max × w) / Σw = max}, bất kể trọng số chia thế nào.
 *
 * <p>Lỗi đã gặp thật trước khi có chốt chặn: 5 tiêu chí mỗi cái tối đa 10 điểm trên thang 0-10,
 * cộng thẳng ra 38.4 rồi bị kẹp còn 10 -- mọi bài đều 10/10.
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
                    "Không thể ban hành: phiên bản này chưa có tiêu chí nào.");
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
                    "Không thể ban hành: phương pháp Trung bình yêu cầu mọi tiêu chí cân bằng ở"
                            + " trọng số 100%%, nên tổng trọng số của %d tiêu chí phải bằng %s."
                            + " Hiện đang là %s.",
                    criteriaCount, expected.toPlainString(), weightSum.toPlainString()));
        }
    }

    private static void assertWeightsSumToOne(int criteriaCount, BigDecimal weightSum) {
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Không thể ban hành: phương pháp Phân bổ yêu cầu tổng trọng số của %d tiêu chí"
                            + " phải bằng 100%% (tức 1), hiện đang là %s (%s%%).",
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
                "Không thể ban hành: tiêu chí \"%s\" có khoảng điểm %s - %s, trong khi thang của"
                        + " phiên bản là %s - %s. Mỗi tiêu chí phải phủ đúng thang; mức quan trọng"
                        + " khác nhau giữa các tiêu chí thể hiện bằng TRỌNG SỐ, không phải bằng"
                        + " khoảng điểm.",
                mismatched.getName(),
                mismatched.getMinScore() == null ? "?" : mismatched.getMinScore().toPlainString(),
                mismatched.getMaxScore() == null ? "?" : mismatched.getMaxScore().toPlainString(),
                scaleMin.toPlainString(),
                scaleMax.toPlainString()));
    }
}
