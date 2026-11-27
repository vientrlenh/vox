package com.sep.vox.domain.common;

import java.time.ZoneId;

/**
 * The BUSINESS time zone of the system. Every concept computed on the local calendar -- "expires after 1 month",
 * "assessment day", "statement period" -- must be converted to this zone before adding or subtracting, because
 * adding in UTC is off by one day for every moment between 17:00 and 23:59 Vietnam time.
 *
 * <p>Placed in domain and not in {@code application.common.DateMapper}: this is a BUSINESS convention that the
 * domain model itself needs (see {@code SubscriptionPlan.endDateFrom}), and domain must not depend upward on
 * application -- see LayeredArchitectureTests. DateMapper points here so the whole system has a single source
 * of truth.
 *
 * <p>Uses {@link ZoneId} rather than {@code ZoneOffset.ofHours(7)}: a fixed offset is wrong in regions with
 * daylight saving time, and {@code atStartOfDay} needs a ZoneId anyway.
 */
public final class ZoneConstant {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ZoneConstant() {
    }
}
