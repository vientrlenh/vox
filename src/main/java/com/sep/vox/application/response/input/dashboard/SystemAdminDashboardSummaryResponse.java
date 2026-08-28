package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Doanh thu ở đây tính trên ĐƠN HÀNG đã thu được tiền (Order.status = SUCCESS), không phải trên hóa
 * đơn: hóa đơn giờ chỉ còn là chứng từ phát hành SAU khi tiền về, không mang trạng thái thanh toán
 * lẫn số tiền nữa -- xem InvoiceRepository.
 */
public record SystemAdminDashboardSummaryResponse(
    long totalSchools,
    long activeSchools,
    long inactiveSchools,
    long pendingRegistrations,
    long registrationsLast30Days,
    long registrationsLast90Days,
    BigDecimal totalRevenue,
    List<MonthlyRevenueResponse> monthlyRevenue,
    long studentCount,
    long teacherCount,
    long schoolAdminCount,
    long activeFrameworkCount,
    long systemRubricCount
) {

}
