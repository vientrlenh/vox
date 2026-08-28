package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;

/**
 * Giá VỐN đang áp dụng, phơi nguyên vẹn cho trường xem -- xem QuotaPricingPort.
 *
 * <p>KHÔNG có serviceFeeRatio: phí dịch vụ là biên lãi của mình và chỉ được xuất hiện dưới dạng một
 * dòng phí đã cộng vào đơn hàng, không phải một trường tra cứu được. Đó cũng là lý do nó nằm ở
 * ServiceFeePort tách riêng chứ không chung cổng với ba con số này.
 *
 * @param estimatedCostPerExamSecondUsd     giá ước tính mỗi giây thi, ưu tiên số đã tự calibrate từ
 *                                          chi phí thật (QuotaPricingCalibrationJob)
 * @param estimatedCostPerPracticeSecondUsd như trên nhưng cho luyện nói -- pipeline AI khác hẳn nên
 *                                          calibrate riêng
 * @param usdToVndRate                      tỷ giá thị trường, KHÔNG cộng lãi: gộp lãi vào đây sẽ ra
 *                                          một tỷ giá trường không đối chiếu được với bất kỳ nguồn
 *                                          nào bên ngoài
 */
public record QuotaPricingResponse(
    BigDecimal estimatedCostPerExamSecondUsd,
    BigDecimal estimatedCostPerPracticeSecondUsd,
    BigDecimal usdToVndRate
) {
}
