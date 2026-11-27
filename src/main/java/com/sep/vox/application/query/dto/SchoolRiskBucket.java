package com.sep.vox.application.query.dto;

/**
 * The four "schools needing attention" buckets on the system overview page.
 *
 * <p>This is NOT a partition -- do not add the four numbers up:
 *
 * <ul>
 *   <li>{@link #EXPIRING_SOON} is a SUBSET of the group of schools that still have a plan.</li>
 *   <li>{@link #LAPSED} and {@link #SUSPENDED} are mutually exclusive and exclude the group that still has a plan.</li>
 *   <li>{@link #IN_DEBT} CUTS ACROSS all three: it reads from the wallet balance and not from the subscription status, so
 *       a school that still has a plan but a negative wallet sits in both places.</li>
 * </ul>
 *
 * <p>Schools that have NEVER bought a plan belong to none of the first three buckets: they have no row in
 * {@code school_subscriptions} to group, so the predicates of all three return NULL and exclude them. That
 * is IDENTICAL to the overview page's count, and keeping it that way is deliberate -- the two screens must
 * first say the same number; whether to add a fifth bucket for them is a separate question.
 */
public enum SchoolRiskBucket {
    /** Still has a plan, but the covering period will end within the warning threshold. */
    EXPIRING_SOON,
    /** Once had a plan, no period covers it now, and it is not suspended. */
    LAPSED,
    /** Suspended and no period covers it now. */
    SUSPENDED,
    /** The top-up wallet is negative -- exactly the schools blocked from opening assessment sessions. */
    IN_DEBT
}
