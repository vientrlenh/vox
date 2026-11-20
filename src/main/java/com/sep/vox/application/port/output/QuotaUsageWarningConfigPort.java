package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Ngưỡng cảnh báo SỚM khi hạn mức AI của trường mới dùng tới một tỉ lệ nào đó của
 * {@code total_allocated_amount_vnd} -- xem ConsumeQuotaService.checkUsageWarningTransition. Giá trị
 * thật bind từ application.yaml ({@code vox.quota.usage-warning.*}) ở QuotaUsageWarningProperties.
 */
public interface QuotaUsageWarningConfigPort {

    /**
     * Bắn cảnh báo khi {@code usedAmountVnd} vừa vượt {@code totalAllocatedAmountVnd * warningRatio}
     * của đúng ví hạn mức (EXAM hoặc PRACTICE) vừa bị trừ. Lấy theo TỶ LỆ trên hạn mức, cùng lý do với
     * {@link QuotaDebtConfigPort#capRatio()}: trường mua gói lớn/nhỏ cần cùng một mốc cảnh báo tương đối.
     */
    BigDecimal warningRatio();
}
