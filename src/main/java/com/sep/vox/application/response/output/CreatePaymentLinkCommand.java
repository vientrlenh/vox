package com.sep.vox.application.response.output;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePaymentLinkCommand(
    String orderRef,
    BigDecimal amount,
    String description,
    /**
     * The link's expiry, TAKEN FROM orders.expires_at and not a constant of the adapter: a link that outlives the
     * order makes the school pay for an expired order, while one shorter than the order leaves the order holding its slot
     * with no way left to pay it.
     */
    Instant expiresAt
) {
    
}
