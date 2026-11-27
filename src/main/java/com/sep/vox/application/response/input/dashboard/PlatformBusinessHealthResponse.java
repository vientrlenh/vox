package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

/**
 * The business health of the platform for the system admin dashboard.
 *
 * <p>Deliberately NOT merged into {@code SystemAdminDashboardSummaryResponse}: that summary already carries 13 fields
 * mixing many concerns, and the project has already split {@code nearestCentralizedAssessment} / {@code questionBankStats}
 * out of {@code schoolAdminDashboard} in exactly this way. Keeping it separate also means the old screen
 * does not pay for the new aggregations, and one broken query does not bring down the whole dashboard.
 *
 * <p>The number of schools is classified AT THE TIME OF THE CALL, not by window: "how many schools still have a plan" changing
 * when you click to last month is meaningless. Money follows the window.
 */
public record PlatformBusinessHealthResponse(
    long subscribedSchools,
    long expiringSoonSchools,
    long lapsedSchools,
    long suspendedSchools,
    long schoolsInDebt,
    /** Money actually collected within the window (SUCCESS orders), VND. */
    BigDecimal revenueVnd,
    /** The same length, immediately BEFORE the window being viewed -- see the use case javadoc for why calendar months are not used. */
    BigDecimal previousRevenueVnd,
    /** The AI cost price incurred within the window, VND. */
    BigDecimal aiCostVnd,
    /** (revenue - AI cost price) / revenue * 100. {@code null} when the window has collected no money. */
    Double grossMarginPercent,
    /**
     * The same formula, computed on the comparison period -- so the screen can draw the change in PERCENTAGE POINTS.
     *
     * <p>Computed here rather than letting the client subtract two ratios it builds itself: the convention "with 0 revenue the margin
     * does not exist, it is not 0%" is only correct if applied once for both periods. A client doing its own division would build
     * a margin of -∞ for an empty previous period and then draw it as a huge drop.
     *
     * <p>{@code null} when the previous period collected no money -- then there is NO change to show.
     */
    Double previousGrossMarginPercent
) {
}
