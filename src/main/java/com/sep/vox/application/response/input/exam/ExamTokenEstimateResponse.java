package com.sep.vox.application.response.input.exam;

import java.math.BigDecimal;

/**
 * Cảnh báo (KHÔNG chặn) ước lượng worst-case chi phí AI + khả năng chi trả còn lại -- xem
 * ClassTestTokenQuotaGuardService.estimateTokenQuota.
 *
 * <p>Tất cả đều là VND theo GIÁ VỐN, không cộng phí dịch vụ: phí đã thu một lần lúc trường đặt đơn
 * (CreateTopUpOrderUseCase), còn ví hạn mức lẫn ví tự nạp đều ghi bằng giá vốn -- xem ServiceFeePort.
 * Cộng phí vào đây sẽ làm ước lượng cao hơn khoản thật sự bị trừ khoảng 20%.
 *
 * @param remainingExamVnd        tiền trường CÒN CHI ĐƯỢC cho ví EXAM = hạn mức kèm gói còn lại +
 *                                số dư ví tự nạp (phần không âm). null nếu trường chưa có
 *                                subscription active hoặc chưa cấu hình ví này
 * @param remainingMyClassTestVnd hạn mức CÁ NHÂN còn lại của giáo viên ra đề -- KHÔNG phải một ví
 *                                cấp trường thứ hai (ví CLASS_TEST đã bị bỏ, xem QuotaType), và
 *                                KHÔNG cộng số dư ví trường vì đây là trần chi nội bộ chứ không
 *                                phải túi tiền. null nếu bài không phải CLASS_TEST hoặc giáo viên
 *                                không được cấp hạn mức riêng, tức chỉ tiền của trường áp dụng
 */
public record ExamTokenEstimateResponse(
    BigDecimal estimatedCostVnd,
    BigDecimal remainingExamVnd,
    BigDecimal remainingMyClassTestVnd,
    boolean wouldExceedExam,
    boolean wouldExceedMyClassTest
) {
}
