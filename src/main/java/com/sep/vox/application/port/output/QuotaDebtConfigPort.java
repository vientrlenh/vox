package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * The WARNING cap for a school's debt -- see ConsumeQuotaService.checkDebtCapTransition. The real
 * values are bound from application.yaml ({@code vox.quota.debt.*}) in QuotaDebtProperties.
 */
public interface QuotaDebtConfigPort {

    /**
     * Debt is considered abnormal when it exceeds {@code total_allocated_amount_vnd * capRatio} of
     * the quota wallet that caused it. It is a RATIO of the quota rather than an absolute amount: for
     * a school that bought a large plan, a debt of a few hundred thousand is normal, while for a
     * school that bought a small plan that same amount is a sign of a bug in the AI cost metering pipeline.
     */
    BigDecimal capRatio();
}
