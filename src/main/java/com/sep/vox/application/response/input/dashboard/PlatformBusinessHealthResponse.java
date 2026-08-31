package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

/**
 * Sức khỏe kinh doanh nền tảng cho dashboard system admin.
 *
 * <p>Cố ý KHÔNG gộp vào {@code SystemAdminDashboardSummaryResponse}: bản tóm tắt đó đã mang 13 trường
 * trộn nhiều mối quan tâm, và dự án vốn đã tách {@code nearestCentralizedExam} / {@code questionBankStats}
 * ra khỏi {@code schoolAdminDashboard} theo đúng cách này. Tách riêng cũng có nghĩa màn hình cũ không
 * phải trả giá cho các phép gộp mới, và một query hỏng không kéo cả dashboard xuống.
 *
 * <p>Số trường phân loại tại THỜI ĐIỂM GỌI, không theo cửa sổ: "bao nhiêu trường đang còn gói" mà đổi
 * khi bấm sang tháng trước thì vô nghĩa. Tiền thì theo cửa sổ.
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
