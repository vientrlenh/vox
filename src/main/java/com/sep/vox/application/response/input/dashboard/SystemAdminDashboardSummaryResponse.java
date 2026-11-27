package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Revenue here is computed on ORDERS that have been paid (Order.status = SUCCESS), not on invoices:
 * an invoice is now only a document issued AFTER the money arrives, and no longer carries payment status
 * or amount -- see InvoiceRepository.
 */
public record SystemAdminDashboardSummaryResponse(
    long totalSchools,
    long activeSchools,
    long inactiveSchools,
    long pendingRegistrations,
    /**
     * The number of days the LONGEST-waiting order has been in the queue. {@code null} when no order is waiting -- very different
     * from 0, which means "there is an order, and it was just submitted today".
     */
    Integer oldestPendingRegistrationDays,
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
