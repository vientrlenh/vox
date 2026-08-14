package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Trần cảnh báo nợ hạn mức AI mà application cần đọc. Giá trị thật bind từ application.yaml
 * ({@code vox.quota.debt.*}) ở QuotaDebtProperties.
 */
public interface QuotaDebtConfigPort {

    BigDecimal capRatio();
}
