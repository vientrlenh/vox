package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.dashboard.MonthlyRevenueResponse;
import com.sep.vox.application.response.input.dashboard.SystemAdminDashboardSummaryResponse;
import com.sep.vox.domain.common.BusinessDays;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

/**
 * Revenue is read from ORDERS that have been paid (Order.status = SUCCESS) instead of from invoices: an invoice is
 * now just a document issued after the money arrives and no longer has a status or amount column -- see
 * InvoiceRepository. This is also a MORE CORRECT number than before: an invoice only exists for a successful
 * order, so "filter PAID invoices" used to filter on a condition that is always true.
 */
@Service
@RequiredArgsConstructor
public class ViewSystemAdminDashboardUseCase implements IUseCase<Void, SystemAdminDashboardSummaryResponse> {

    private static final String STUDENT_CODE = "STUDENT";
    private static final String TEACHER_CODE = "TEACHER";
    private static final String SCHOOL_ADMIN_CODE = "SCHOOL_ADMIN";

    /** Number of months drawn on the revenue chart, including the current month. */
    private static final int REVENUE_MONTHS = 24;

    private final SchoolRepository schoolRepository;
    private final RegisterFormRepository registerFormRepository;
    private final OrderRepository orderRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final FrameworkRepository frameworkRepository;
    private final RubricRepository rubricRepository;

    @Override
    @Transactional(readOnly = true)
    public SystemAdminDashboardSummaryResponse execute(Void input) {
        Instant now = Instant.now();
        long totalSchools = schoolRepository.countAll();
        long activeSchools = schoolRepository.countByIsActiveTrue();

        BigDecimal totalRevenue = totalRevenueUntil(now);

        return new SystemAdminDashboardSummaryResponse(
            totalSchools,
            activeSchools,
            totalSchools - activeSchools,
            registerFormRepository.countByStatus(RegisterFormStatus.PENDING),
            oldestPendingRegistrationDays(now),
            registerFormRepository.countByCreatedAtAfter(now.minus(30, ChronoUnit.DAYS)),
            registerFormRepository.countByCreatedAtAfter(now.minus(90, ChronoUnit.DAYS)),
            totalRevenue,
            buildMonthlyRevenue(now),
            countUsersByRole(STUDENT_CODE),
            countUsersByRole(TEACHER_CODE),
            countUsersByRole(SCHOOL_ADMIN_CODE),
            frameworkRepository.findAllActive(1, 1).totalElements(),
            rubricRepository.findAllByOwnerType(RubricOwnerType.SYSTEM, 1, 1).totalElements()
        );
    }

    // Summed in the DB instead of loading the orders to count them: this is an ALL-TIME figure, and the list behind
    // it only grows. The lower bound is EPOCH because it means "everything"; the interval is half-open [from, to),
    // so the upper bound is now, i.e. up to the moment the screen is opened.
    private BigDecimal totalRevenueUntil(Instant now) {
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatusInRange(OrderStatus.SUCCESS, Instant.EPOCH, now);
        return totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    /**
     * How many CALENDAR DAYS the longest-waiting order has been in the queue, in business time.
     *
     * <p>Counted in calendar days rather than elapsed divided by 24 hours: an order submitted at 23:00 yesterday that
     * is read at 01:00 is "already on the second day" to whoever is on duty, while dividing by 24 hours still returns 0
     * and makes the KPI card look as if the queue is still clean.
     *
     * <p>{@code null} when no order is waiting -- the same convention as {@code grossMarginPercent}: missing data and
     * "equal to zero" are two different things, and here 0 is the BEST state, so mixing the two would draw an empty
     * queue that looks exactly like a queue that was just cleared.
     */
    private Integer oldestPendingRegistrationDays(Instant now) {
        return registerFormRepository.findOldestCreatedAtByStatus(RegisterFormStatus.PENDING)
            .map(oldest -> BusinessDays.waitedDaysSince(oldest, now))
            .orElse(null);
    }

    private long countUsersByRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .map(role -> userRoleRepository.countByRoleId(role.getId()))
            .orElse(0L);
    }

    /**
     * Revenue of the last 24 months (including the current month), ordered old -> new, with 0 for a month that has no
     * paid order.
     *
     * <p>Loads only the 24-month window instead of every successful order: the old version loaded all PAID invoices
     * and threw away the part outside the window, so every year that passed made the screen heavier while the
     * result did not change.
     *
     * <p>Groups by the month of {@code createdAt} -- the same reference point as sumTotalAmountByStatusInRange above,
     * so the total of the chart and the total revenue figure do not tell two different stories. Converted to Vietnam
     * time before cutting the month: cutting in UTC would push the first 7 hours of every month into the previous
     * month.
     */
    private List<MonthlyRevenueResponse> buildMonthlyRevenue(Instant now) {
        YearMonth currentMonth = YearMonth.now(ZoneConstant.BUSINESS_ZONE);
        Map<YearMonth, BigDecimal> totalsByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (int i = REVENUE_MONTHS - 1; i >= 0; i--) {
            totalsByMonth.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }

        Instant windowStart = currentMonth.minusMonths(REVENUE_MONTHS - 1L)
            .atDay(1).atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();

        for (Order order : orderRepository.findByStatusInRange(OrderStatus.SUCCESS, windowStart, now)) {
            YearMonth orderMonth = YearMonth.from(order.getCreatedAt().atZone(ZoneConstant.BUSINESS_ZONE));
            totalsByMonth.computeIfPresent(orderMonth, (month, total) -> total.add(order.getTotalAmountVnd()));
        }

        return totalsByMonth.entrySet().stream()
            .map(entry -> new MonthlyRevenueResponse(entry.getKey().toString(), entry.getValue()))
            .toList();
    }

}
