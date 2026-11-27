package com.sep.vox.domain.model.payment;

import com.sep.vox.domain.model.order.OrderStatus;

/**
 * Lifecycle of ONE PAYMENT ATTEMPT, not of the order.
 *
 * <p>PENDING is created at the moment the link is created (nobody has paid yet), then moves to PAID/FAILED when the
 * gateway reports back or when we ask again ourselves. PENDING must exist to keep the provider_order_ref of the
 * pending attempt -- without it a webhook cannot trace a callback back to an order, and before issuing a new
 * link we cannot ask whether the old attempt was already paid (a path to charging twice).
 *
 * <p>DELIBERATELY no EXPIRED/CANCELLED here: for an attempt there is only "money came in" or "money did not come
 * in". The nuance of why money did not come in belongs to the order -- see
 * {@link OrderStatus} (CANCELLED/EXPIRED/FAILED).
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED
}
