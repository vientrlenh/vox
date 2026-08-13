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
        if (method == RubricTotalScoreMethod.SUM && weightSum.compareTo(BigDecimal.ZERO) > 0) {
            // SUM = TỔNG CÓ TRỌNG SỐ (đổi 2026-08-13). Mỗi tiêu chí chấm trên CẢ THANG rồi nhân
            // trọng số của nó: 5 tiêu chí thang 0-10 trọng số 0.2 -> 10x0.2x5 = 10, vừa đúng thang.
            //
            // Bản cũ cộng thẳng và BỎ QUA trọng số, nên muốn vừa thang phải khai mỗi tiêu chí
            // 0-2. Cách đó có hai cái giá: giáo viên phải nghĩ "câu này mấy trên 2" thay vì trên
            // thang quen thuộc, và cột trọng số vẫn bắt buộc nhập nhưng không ai đọc -- rubric
            // hiện "20%" trong khi chấm theo tỉ lệ khác hẳn mà không gì phát hiện được.
            //
            // Điều kiện để không vượt thang là Σweight = 1, ép lúc PUBLISH rubric.
            resolved = weightedSum;
        } else if (method == RubricTotalScoreMethod.SUM) {
            // SUM mà không tiêu chí nào khai trọng số -> lùi về tổng thẳng như bản cũ, để rubric
            // cũ chấm lại vẫn ra đúng con số cũ thay vì đổi lặng lẽ.
            resolved = plainSum;
        } else {
            // WEIGHTED_AVERAGE = TRUNG BÌNH CỘNG các điểm tiêu chí, KHÔNG nhân trọng số.
            //
            // Tên gọi dễ gây hiểu nhầm, nhưng đây là ngữ nghĩa nhà trường dùng: mỗi tiêu chí chấm
            // trên cả thang rồi lấy trung bình, ai cũng đọc ra ngay. Muốn phân bổ tỉ trọng khác
            // nhau giữa các tiêu chí thì dùng SUM -- đó đúng là chỗ trọng số có tác dụng.
            //
            // Hai phương pháp TRÙNG kết quả khi mọi trọng số bằng nhau (0.2 x 5 = trung bình), và
            // chỉ tách ra khi trọng số lệch: 0.5/0.2/0.1/0.1/0.1 với điểm 10/10/10/10/6 cho
            // SUM = 9.6 còn WEIGHTED_AVERAGE = 9.2.
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
