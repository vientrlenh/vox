package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Khớp với input SubscriptionPlanQuotaInput trong subscription.graphqls. quotaType để nguyên kiểu
 * enum vì schema đã khai QuotaType! -- graphql-java từ chối giá trị lạ ngay từ khâu parse, nên tới
 * đây thì giá trị chắc chắn hợp lệ.
 */
public record SubscriptionPlanQuotaInput(
    QuotaType quotaType,
    BigDecimal includedAmountVnd
) {
}
