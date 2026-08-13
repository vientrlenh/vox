package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionQuotaDto(
    UUID id,
    UUID subscriptionId,
    String quotaType,
    BigDecimal totalAllocated,
    BigDecimal usedQuantity,
    // usedQuantity > totalAllocated -- với GRADING/CLASS_TEST, đây chính là điều kiện khóa cấp
    // trường (xem SchoolSubscriptionDebtGuardService). Với PRACTICE chỉ mang tính thông tin, không
    // khóa gì (luồng PRACTICE vẫn chặn cứng ở ConsumeQuotaUseCase, không cho phép đi vào trạng thái này).
    boolean isLocked
) {
}
