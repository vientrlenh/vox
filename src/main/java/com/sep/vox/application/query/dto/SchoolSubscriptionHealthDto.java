package com.sep.vox.application.query.dto;

/**
 * Classifies EACH SCHOOL by the state of its service plan on one date, each school falling into exactly one bucket.
 *
 * <p>It must be classified per school rather than counted per {@code school_subscriptions} row: a school
 * has many subscription periods in its history, so counting rows would both double-count and put a
 * school that is using the service fine into the "expired" bucket just because last year's period has ended.
 *
 * <p>"In effect" includes {@code CANCELLED}: that status only turns off auto-renewal, and the school
 * can keep using the service until the end of {@code endDate} -- see {@code SchoolSubscriptionStatus}.
 * Only {@code SUSPENDED} means losing access immediately.
 */
public record SchoolSubscriptionHealthDto(
    /** Schools that have a subscription period covering the date being examined. */
    long subscribedSchools,
    /** A subset of {@code subscribedSchools}: the current covering period ends within the warning threshold. */
    long expiringSoonSchools,
    /** Once had a plan but no period covers it now, and it is not suspended. */
    long lapsedSchools,
    /** Suspended by a system admin and no period covers it now. */
    long suspendedSchools
) {
}
