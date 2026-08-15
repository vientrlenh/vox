package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;
import java.util.List;

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
     * @param criteria toàn bộ tiêu chí của version; rỗng thì ném lỗi vì phiên bản không có tiêu chí
     *                 không dùng để chấm được
     */
    public static void assertPublishable(
            RubricTotalScoreMethod method,
            BigDecimal scaleMin,
            BigDecimal scaleMax,
            List<RubricCriterion> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalStateException(
                    "Không thể ban hành: phiên bản này chưa có tiêu chí nào.");
        }

        assertEveryCriterionCoversScale(scaleMin, scaleMax, criteria);

        var weightSum = criteria.stream()
                .map(criterion -> criterion.getWeight())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        if (method == RubricTotalScoreMethod.WEIGHTED_AVERAGE) {
            // Trung bình: mọi tiêu chí cân bằng ở 100% -> tổng bằng đúng số tiêu chí.
            //
            // Dùng mốc "bằng số tiêu chí" thay vì "bằng 1" là có chủ ý: cột weight là numeric(6,2),
            // nên chia đều theo mốc 1 thì 3 tiêu chí ra 0.33+0.33+0.33 = 0.99, không bao giờ khớp,
            // buộc phải gõ lệch 0.34/0.33/0.33. Theo mốc n thì cả ba đều là 1.00, khớp chằn chặn
            // với mọi số tiêu chí, và thêm tiêu chí mới không phải viết lại các tiêu chí cũ.
            var expected = BigDecimal.valueOf(criteria.size());
            if (weightSum.compareTo(expected) != 0) {
                throw new IllegalStateException(String.format(
                        "Không thể ban hành: phương pháp Trung bình yêu cầu mọi tiêu chí cân bằng ở"
                                + " trọng số 100%%, nên tổng trọng số của %d tiêu chí phải bằng %s."
                                + " Hiện đang là %s.",
                        criteria.size(), expected.toPlainString(), weightSum.toPlainString()));
            }
            return;
        }

        // Phân bổ: trọng số là phần chia của tiêu chí trong tổng, cộng lại đúng 100%.
        if (weightSum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalStateException(String.format(
                    "Không thể ban hành: phương pháp Phân bổ yêu cầu tổng trọng số của %d tiêu chí"
                            + " phải bằng 100%% (tức 1), hiện đang là %s (%s%%).",
                    criteria.size(), weightSum.toPlainString(),
                    weightSum.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()));
        }
    }

    /**
     * Mỗi tiêu chí phải phủ ĐÚNG thang. Trung bình có trọng số của các giá trị trong [a,b] thì nằm
     * trong [a,b] -- nhưng chỉ khi MỌI tiêu chí cùng nằm trong [a,b] đó. Lệch một trong hai đầu là
     * hỏng theo hai kiểu:
     *
     * <ul>
     *   <li>tiêu chí RỘNG hơn thang (thang 4-10, tiêu chí 0-100): điểm vọt lên tới 100 rồi bị kẹp
     *       còn 10, mọi bài khá trở lên đều thành 10.
     *   <li>tiêu chí HẸP hơn thang (thang 4-10, một tiêu chí 4-5): học sinh làm tối đa mọi thứ vẫn
     *       không bao giờ chạm được 10, trần thang thành trần chết.
     * </ul>
     *
     * <p>Chênh lệch giữa các tiêu chí thể hiện bằng TRỌNG SỐ, không phải bằng khoảng điểm.
     */
    private static void assertEveryCriterionCoversScale(
            BigDecimal scaleMin, BigDecimal scaleMax, List<RubricCriterion> criteria) {
        var mismatched = criteria.stream()
                .filter(criterion -> criterion.getMinScore() == null
                        || criterion.getMaxScore() == null
                        || criterion.getMinScore().compareTo(scaleMin) != 0
                        || criterion.getMaxScore().compareTo(scaleMax) != 0)
                .findFirst()
                .orElse(null);
        if (mismatched == null) {
            return;
        }
        throw new IllegalStateException(String.format(
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
