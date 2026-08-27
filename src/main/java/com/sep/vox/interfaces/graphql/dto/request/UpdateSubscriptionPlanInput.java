package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Khớp với input UpdateSubscriptionPlanInput trong subscription.graphqls: mọi field đều nullable với
 * nghĩa "không đụng tới". Không có periodType (cột period_type là updatable = false) và không có
 * serviceFeeRatio (đã thành config toàn hệ thống) -- xem {@link
 * com.sep.vox.application.port.input.command.UpdateSubscriptionPlanCommand}.
 */
public record UpdateSubscriptionPlanInput(
    String name,
    String tagline,
    BigDecimal priceVnd,
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    List<SubscriptionPlanQuotaInput> quotas
) {
}
