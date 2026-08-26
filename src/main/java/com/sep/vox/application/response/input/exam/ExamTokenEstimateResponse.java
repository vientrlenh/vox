package com.sep.vox.application.response.input.exam;

import java.math.BigDecimal;

/**
 * Cảnh báo (KHÔNG chặn) ước lượng worst-case chi phí AI + hạn mức còn lại -- xem
 * ClassTestTokenQuotaGuardService.estimateTokenQuota.
 *
 * @param remainingExamUsd         ví EXAM cấp trường còn lại; null nếu trường chưa có subscription
 *                                 active hoặc chưa cấu hình ví này
 * @param remainingMyClassTestUsd  hạn mức CÁ NHÂN còn lại của giáo viên ra đề -- KHÔNG phải một ví
 *                                 cấp trường thứ hai (ví CLASS_TEST đã bị bỏ, xem QuotaType). null
 *                                 nếu bài không phải CLASS_TEST hoặc giáo viên không được cấp hạn
 *                                 mức riêng, tức chỉ ví của trường áp dụng
 */
public record ExamTokenEstimateResponse(
    BigDecimal estimatedCostUsd,
    BigDecimal remainingExamUsd,
    BigDecimal remainingMyClassTestUsd,
    boolean wouldExceedExam,
    boolean wouldExceedMyClassTest
) {
}
