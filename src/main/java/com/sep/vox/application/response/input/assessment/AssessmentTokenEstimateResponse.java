package com.sep.vox.application.response.input.assessment;

import java.math.BigDecimal;

/**
 * Cảnh báo (KHÔNG chặn) ước lượng worst-case chi phí AI + khả năng chi trả còn lại -- xem
 * ClassTestTokenQuotaGuardService.estimateTokenQuota.
 *
 * <p>Tất cả đều là VND theo GIÁ VỐN, không cộng phí dịch vụ: phí đã thu một lần lúc trường đặt đơn
 * (CreateTopUpOrderUseCase), còn ví hạn mức lẫn ví tự nạp đều ghi bằng giá vốn -- xem ServiceFeePort.
 * Cộng phí vào đây sẽ làm ước lượng cao hơn khoản thật sự bị trừ khoảng 20%.
 *
 * @param remainingAssessmentVnd        tiền trường CÒN CHI ĐƯỢC cho ví ASSESSMENT = hạn mức kèm gói còn lại +
 *                                số dư ví tự nạp (phần không âm). null nếu trường chưa có
 *                                subscription active hoặc chưa cấu hình ví này
 * @param remainingMyClassTestVnd hạn mức CÁ NHÂN còn lại của giáo viên ra đề -- KHÔNG phải một ví
 *                                cấp trường thứ hai (ví CLASS_TEST đã bị bỏ, xem QuotaType), và
 *                                KHÔNG cộng số dư ví trường vì đây là trần chi nội bộ chứ không
 *                                phải túi tiền. null nếu bài không phải CLASS_TEST hoặc giáo viên
 *                                không được cấp hạn mức riêng, tức chỉ tiền của trường áp dụng
 * @param schoolLocked            trường đang bị khóa do nợ (số dư ví tự nạp âm) -- xem
 *                                SchoolSubscriptionDebtGuardService.isSchoolLocked. Nguyên nhân
 *                                KHÁC với wouldExceedAssessment (hết hạn mức): có thể true dù
 *                                wouldExceedAssessment=false nếu hạn mức gói vẫn còn dư, vì
 *                                remainingAssessmentVnd cố ý clamp số dư âm về 0 (xem
 *                                ClassTestTokenQuotaGuardService.spendableSchoolFundsVnd)
 * @param sharedPoolUsageRatio    % ước lượng chi phí bài này chiếm trong PHẦN HẠN MỨC GÓI còn lại
 *                                của ví ASSESSMENT (không cộng số dư ví tự nạp) -- chỉ có giá trị khi
 *                                assessment.kind == CENTRALIZED và phần hạn mức gói còn dư > 0. CENTRALIZED
 *                                không có hạn mức cá nhân nên dễ "ăn" vào phần ví chung mà giáo viên
 *                                khác (đang có hạn mức cá nhân riêng cho CLASS_TEST) không hề biết
 *                                trước -- xem teachersWithUnusedPersonalAllocationCount. null cho
 *                                CLASS_TEST hoặc khi không áp dụng được (chưa có subscription, hoặc
 *                                ví gói đã cạn sẵn -- lúc đó wouldExceedAssessment/schoolLocked đã đủ nói)
 * @param teachersWithUnusedPersonalAllocationCount số giáo viên (thuộc subscription này) hiện còn
 *                                hạn mức cá nhân CHƯA dùng hết cho CLASS_TEST (quotaType=ASSESSMENT). Chỉ
 *                                có giá trị cùng điều kiện với sharedPoolUsageRatio
 */
public record AssessmentTokenEstimateResponse(
    BigDecimal estimatedCostVnd,
    BigDecimal remainingAssessmentVnd,
    BigDecimal remainingMyClassTestVnd,
    boolean wouldExceedAssessment,
    boolean wouldExceedMyClassTest,
    boolean schoolLocked,
    BigDecimal sharedPoolUsageRatio,
    Integer teachersWithUnusedPersonalAllocationCount
) {
}
