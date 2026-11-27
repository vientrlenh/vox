package com.sep.vox.domain.model.assessment;

/**
 * The teacher's conclusion when closing an assignment row.
 *
 * <p>Four actions shared across all four {@link GradingRoundType}s — the key finding of the rework:
 * "release a result without changing the score" and "spot check and keep as is" are the same thing
 * (confirming the current score is correct), so they are merged into {@link #UPHELD}.
 *
 * <p>The ratio {@link #UPHELD} / {@link #REGRADED} in the SPOT_CHECK round is itself the measure of AI
 * quality, with nothing extra to collect.
 */
public enum GradingOutcome {
    /** Keep the current score. */
    UPHELD,
    /** Re-grade, the score changes. */
    REGRADED,
    /** Concludes a violation -> the result becomes INVALID. A reason is required. */
    INVALIDATED,
    /** Concludes no violation -> lifts INVALID. A reason is required. */
    CLEARED_INVALID,
    /** The teacher hands the assignment back (knows the candidate...). A reason is required. */
    DECLINED
}
