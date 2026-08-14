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
     * Giá bán mỗi $1 hạn mức cho CreatePlanUseCase/UpdatePlanUseCase -- tính từ usdToVndRate() ×
     * (1 + serviceFeeRatio), làm tròn HALF_UP về số nguyên (khớp cột plan_quota.token_unit_price
     * NUMERIC(15,0)).
     */
    BigDecimal tokenUnitPriceFor(BigDecimal serviceFeeRatio);
}
