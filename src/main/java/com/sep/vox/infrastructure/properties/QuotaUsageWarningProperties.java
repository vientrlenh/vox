package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.QuotaUsageWarningConfigPort;

/**
 * Ngưỡng cảnh báo SỚM (KHÔNG chặn) khi hạn mức AI của trường (EXAM/PRACTICE) mới dùng tới một tỉ lệ
 * nào đó -- xem ConsumeQuotaService.checkUsageWarningTransition. Mục đích là để nhà trường biết trước
 * mà gia hạn/nạp thêm, TRƯỚC KHI hạn mức cạn và bắt đầu tính vào nợ (xem QuotaDebtProperties).
 */
@ConfigurationProperties(prefix = "vox.quota.usage-warning")
public record QuotaUsageWarningProperties(
    BigDecimal warningRatio
) implements QuotaUsageWarningConfigPort {

    private static final BigDecimal DEFAULT_WARNING_RATIO = new BigDecimal("0.70");

    public QuotaUsageWarningProperties {
        warningRatio = warningRatio == null ? DEFAULT_WARNING_RATIO : warningRatio;
    }
}
