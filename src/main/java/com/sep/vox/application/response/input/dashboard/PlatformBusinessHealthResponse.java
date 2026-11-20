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
    /** Tiền thực thu trong cửa sổ (đơn hàng SUCCESS), VND. */
    BigDecimal revenueVnd,
    /** Cùng độ dài, ngay TRƯỚC cửa sổ đang xem — xem javadoc use case về lý do không dùng tháng lịch. */
    BigDecimal previousRevenueVnd,
    /** Giá vốn AI phát sinh trong cửa sổ, VND. */
    BigDecimal aiCostVnd,
    /** (doanh thu - giá vốn AI) / doanh thu * 100. {@code null} khi cửa sổ chưa thu được đồng nào. */
    Double grossMarginPercent,
    /**
     * Cùng công thức, tính trên kỳ so sánh — để màn hình vẽ được mức chênh theo ĐIỂM PHẦN TRĂM.
     *
     * <p>Tính ở đây chứ không để client lấy hiệu của hai tỷ lệ tự dựng: quy ước "doanh thu 0 thì biên
     * không tồn tại, không phải 0%" chỉ đúng nếu áp một lần cho cả hai kỳ. Client tự chia sẽ dựng ra
     * một biên -∞ cho kỳ trước trống rồi vẽ thành cú sụt khổng lồ.
     *
     * <p>{@code null} khi kỳ trước chưa thu được đồng nào — lúc đó KHÔNG có mức chênh để hiển thị.
     */
    Double previousGrossMarginPercent
) {
}
