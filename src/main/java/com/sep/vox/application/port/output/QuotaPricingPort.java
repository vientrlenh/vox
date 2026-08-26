package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Cổng ĐỌC pricing config cho tầng application/interfaces -- nguồn số thật (properties trong .env +
 * snapshot trong DB) nằm ở infrastructure, nên các use case chỉ phụ thuộc vào interface này thay vì
 * import ngược lên implementation (xem QuotaPricingService).
 *
 * <p>Mọi method đều đã tự xử lý fallback bên trong implementation (giá calibrate -> hằng số .env,
 * snapshot tỷ giá -> hằng số .env) nên luôn trả về giá trị dùng được, không bao giờ null.
 */
public interface QuotaPricingPort {

    /** Giá ước tính mỗi giây thi (USD) -- dùng cho ClassTestTokenQuotaGuardService. */
    BigDecimal currentEstimatedCostPerExamSecondUsd();

    /** Giá ước tính mỗi giây luyện tập (USD) -- dùng cho BuildPracticePaperUseCase. */
    BigDecimal currentEstimatedCostPerPracticeSecondUsd();

    /** Tỷ giá USD->VND thị trường đang áp dụng, dùng chung cho mọi gói. */
    BigDecimal usdToVndRate();

    /**
     * Tỷ lệ phí dịch vụ (vd. 0.20 = 20%) mà đơn hàng cộng thêm vào phần tiền hạn mức.
     *
     * <p>CỐ Ý không gộp sẵn vào tỷ giá: gộp thì ra "1 USD = 31.200đ" trong khi tỷ giá thật là
     * 26.000đ, trường không đối chiếu được với bất kỳ tỷ giá nào ngoài thị trường. Phí phải đứng
     * thành một dòng riêng trên đơn để nhìn là biết mình đang trả cái gì.
     *
     * <p>Là config TOÀN HỆ THỐNG chứ không lưu theo từng gói (cột service_fee_ratio đã bị bỏ khỏi
     * SubscriptionPlan) -- xem QuotaSellingPriceProperties.
     */
    BigDecimal serviceFeeRatio();
}
