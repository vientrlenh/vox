package com.sep.vox.domain.service.exam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * Gộp điểm các tiêu chí thành điểm MỘT câu, theo phương pháp khai trong rubric version.
 *
 * <p>Gộp 2026-08-11 từ hai bản chép tay của cùng công thức:
 * {@code RecordExamAttemptEvaluationUseCase.computeItemScore} (AI chấm) và
 * {@code GradingItemScoreResolver.itemScore} (giáo viên chấm tay). Hai bản đã trôi lệch ở mẫu số
 * của nhánh dự phòng -- bản AI chia cho số tiêu chí KHỚP được rubric, bản chấm tay chia cho số
 * tiêu chí NỘP LÊN -- nên cùng một bài chấm bằng hai đường có thể ra hai điểm khác nhau.
 *
 * <p>Chỉ gộp phần CÔNG THỨC. Phần phân giải và kiểm tra đầu vào vẫn nằm ở mỗi bên gọi, vì chúng
 * khác nhau có lý do: điểm giáo viên nhập tay phải bị từ chối khi thiếu tiêu chí bắt buộc hoặc
 * lọt ra ngoài khoảng, còn payload của AI thì phải suy giảm êm (bỏ qua tiêu chí lạ) chứ không
 * được làm hỏng cả lượt chấm.
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
     * @param method   phương pháp khai trong rubric version
     * @param scaleMin thang điểm của rubric version -- kết quả luôn bị kẹp về trong thang
     */
    public static BigDecimal compute(
            List<ScoredCriterion> scored,
            RubricTotalScoreMethod method,
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
        if (method == RubricTotalScoreMethod.SUM) {
            // SUM nghĩa là "cộng các phần điểm thành phần": mỗi tiêu chí chiếm một lát của thang
            // (VD thang 10 = 2+2+2+2+2), nên trọng số KHÔNG tham gia -- lát điểm đã chính là
            // trọng số rồi. Điều kiện để phép cộng này nằm trong thang là tổng max của các tiêu
            // chí bằng đúng trần thang; ràng buộc đó được ép lúc PUBLISH rubric
            // (ChangeSchoolRubricVersionStatusUseCase), không kiểm lại ở đây.
            resolved = plainSum;
        } else if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
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
