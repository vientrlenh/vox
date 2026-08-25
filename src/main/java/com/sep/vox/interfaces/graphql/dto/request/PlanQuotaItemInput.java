package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

public record PlanQuotaItemInput(
    QuotaType quotaType,
    BigDecimal includedQuantity
) {
}
