package com.sep.vox.domain.model.assessment;

/**
 * How the admin chooses the set of results to assign automatically for a {@link GradingRoundType grading round}.
 *
 * <p>Four modes exist for four different needs: first grading takes everything, spot check samples (nobody
 * spot-checks 100% of results), inspection targets suspicious results, and handling edge cases is a manual
 * selection.
 */
public enum GradingSampleSelectionMode {
    /** Every eligible result that has nobody yet. Default for the INITIAL round. */
    ALL,
    /** Randomly picks N% — used for periodic spot checks. */
    RANDOM_PERCENT,
    /** Prioritises high-risk results: low AI confidence, or a score close to the passing threshold. */
    RISK_BASED,
    /** A list of results supplied by the admin. */
    MANUAL_LIST
}
