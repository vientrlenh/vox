package com.sep.vox.domain.service.exam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * <p>Bỏ nhánh riêng của {@code SUM} 2026-08-14. Trước đó {@code SUM} nghĩa là "mỗi tiêu chí chiếm
 * một lát của thang (10 = 2+2+2+2+2), cộng thẳng không qua trọng số" -- và ở kiểu khai đó trọng số
 * là dữ liệu chết: không tham gia phép tính, cũng không được kiểm lúc publish, nên con số hiện trên
 * giao diện có thể hoàn toàn bịa. Nay cả hai phương pháp đều chấm mỗi tiêu chí trên TOÀN thang và
 * gộp bằng cùng biểu thức dưới đây; chúng chỉ khác nhau ở ràng buộc tổng trọng số lúc publish (xem
 * {@code RubricScoringConsistencyValidator}), tức khác về CÁCH KHAI chứ không khác về phép tính.
 *
 * <p>Biểu thức {@code Σ(điểm × trọng số) / Σtrọng số} bất biến với việc nhân tỉ lệ toàn bộ trọng
 * số, nên nó đúng cho cả hai ràng buộc: phân bổ ({@code Σw = 1}) chia cho 1 nên quy về tổng có
 * trọng số, còn trung bình ({@code Σw = n}) chia cho n nên quy về trung bình cộng.
 */
public final class RubricItemScoreFormula {

    private RubricItemScoreFormula() {
    }

    /**
     * Một tiêu chí ĐÃ được phân giải: điểm đã kẹp trong khoảng của chính tiêu chí đó, kèm trọng
     * số khai trong rubric.
     *
     * @param weight null coi như không khai trọng số -- cột {@code rubric_criterions.weight} là
     *               NOT NULL nên trong thực tế không xảy ra, nhưng công thức không nên phụ thuộc
     *               vào ràng buộc của một bảng cụ thể.
     */
    public record ScoredCriterion(BigDecimal score, BigDecimal weight) {
    }

    /**
     * @param scored   các tiêu chí đã phân giải; rỗng thì trả về {@code scaleMin}
     * @param scaleMin thang điểm của rubric version -- kết quả luôn bị kẹp về trong thang
     */
    public static BigDecimal compute(
            List<ScoredCriterion> scored,
            BigDecimal scaleMin,
            BigDecimal scaleMax) {
        if (scored == null || scored.isEmpty()) {
            return clampToScale(scaleMin == null ? BigDecimal.ZERO : scaleMin, scaleMin, scaleMax);
        }

        var plainSum = BigDecimal.ZERO;
        var weightedSum = BigDecimal.ZERO;
        var weightSum = BigDecimal.ZERO;
        for (var item : scored) {
            if (item == null || item.score() == null) {
                continue;
            }
            plainSum = plainSum.add(item.score());
            if (item.weight() != null) {
                weightedSum = weightedSum.add(item.score().multiply(item.weight()));
                weightSum = weightSum.add(item.weight());
            }
        }

        BigDecimal resolved;
        if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
            resolved = weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
        } else {
            // Không tiêu chí nào khai trọng số -> trung bình cộng. Lưới an toàn cho dữ liệu cũ
            // có trước khi có ràng buộc lúc publish; vẫn cho ra điểm nằm trong thang.
            resolved = plainSum.divide(BigDecimal.valueOf(scored.size()), 2, RoundingMode.HALF_UP);
        }
        return clampToScale(resolved, scaleMin, scaleMax);
    }

    private static BigDecimal clampToScale(BigDecimal score, BigDecimal scaleMin, BigDecimal scaleMax) {
        var safe = score == null ? BigDecimal.ZERO : score;
        if (scaleMin != null && safe.compareTo(scaleMin) < 0) {
            safe = scaleMin;
        }
        if (scaleMax != null && safe.compareTo(scaleMax) > 0) {
            safe = scaleMax;
        }
        return safe.setScale(2, RoundingMode.HALF_UP);
    }
}
