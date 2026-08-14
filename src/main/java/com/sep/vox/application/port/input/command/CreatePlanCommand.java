package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;

public record CreatePlanCommand(
    String name,
    String tagline,
    BigDecimal pricePerYear,
    Integer validityDays,
    Integer maxTimePerAttemptMin,
    Integer maxStudentCount,
    // null -> CreatePlanUseCase áp mặc định 0.20 (20%). Margin dịch vụ riêng của gói này, không
    // phải config toàn hệ thống -- xem QuotaSellingPriceProperties.
    BigDecimal serviceFeeRatio,
    List<PlanQuotaInput> quotas
) {
}
