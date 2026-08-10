package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record SystemAdminDashboardSummaryDto(
    long totalSchools,
    long activeSchools,
    long inactiveSchools,
    long pendingRegistrations,
    long registrationsLast30Days,
    long registrationsLast90Days,
    BigDecimal totalRevenue,
    List<MonthlyRevenueDto> monthlyRevenue,
    long studentCount,
    long teacherCount,
    long schoolAdminCount,
    long activeFrameworkCount,
    long systemRubricCount
) {

    public record MonthlyRevenueDto(
        String month,
        BigDecimal amount
    ) {

    }

}
