package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.ViewPlatformBusinessHealthQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.application.response.input.dashboard.PlatformBusinessHealthResponse;
import com.sep.vox.domain.common.BusinessConstant;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.repository.OrderRepository;

/**
 * Trường còn gói / sắp rụng / đang nợ, cùng doanh thu đặt cạnh giá vốn AI.
 *
 * <p>Doanh thu đọc từ ĐƠN HÀNG đã thu tiền ({@code OrderStatus.SUCCESS}) — cùng nguồn với
 * {@link ViewSystemAdminDashboardUseCase}, để hai chỗ trên cùng một màn hình không nói hai con số
 * khác nhau.
 */
@Service
@RequiredArgsConstructor
public class ViewPlatformBusinessHealthUseCase
        implements IUseCase<ViewPlatformBusinessHealthQuery, PlatformBusinessHealthResponse> {

    private final PlatformBusinessHealthQueryRepository platformBusinessHealthQueryRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PlatformBusinessHealthResponse execute(ViewPlatformBusinessHealthQuery input) {
        ZoneId zone = ZoneConstant.BUSINESS_ZONE;
        Instant now = Instant.now();

        // The threshold slides with the current instant and is NOT anchored to a calendar day: end_date/start_date
        // are timestamptz columns, so "30 days from now" computed as an instant is the only comparison that does
        // not have to guess a time zone.
        SchoolSubscriptionHealthDto health = platformBusinessHealthQueryRepository.countSchoolSubscriptionHealth(
            now, now.plus(BusinessConstant.DASHBOARD_EXPIRING_SOON_DAYS, ChronoUnit.DAYS));
        long schoolsInDebt = platformBusinessHealthQueryRepository.countSchoolsInDebt();

        Instant to = input == null || input.dateTo() == null ? now : input.dateTo();
        Instant from = input == null || input.dateFrom() == null
            ? YearMonth.now(zone).atDay(1).atStartOfDay(zone).toInstant()
            : input.dateFrom();

        if (!from.isBefore(to)) {
            return responseWithoutMoney(health, schoolsInDebt);
        }

        return responseWithMoney(health, schoolsInDebt, from, to);
    }

    // Empty or reversed range: the school counts are still right because they do not depend on the window; the
    // money part falls back to 0.
    private PlatformBusinessHealthResponse responseWithoutMoney(SchoolSubscriptionHealthDto health, long schoolsInDebt) {
        return new PlatformBusinessHealthResponse(
            health.subscribedSchools(), health.expiringSoonSchools(), health.lapsedSchools(),
            health.suspendedSchools(), schoolsInDebt,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    private PlatformBusinessHealthResponse responseWithMoney(
            SchoolSubscriptionHealthDto health, long schoolsInDebt, Instant from, Instant to) {
        BigDecimal revenue = zeroIfNull(orderRepository.sumTotalAmountByStatusInRange(OrderStatus.SUCCESS, from, to));
        BigDecimal aiCost = zeroIfNull(platformBusinessHealthQueryRepository.sumAiCostVnd(from, to));

        // The comparison period is a window of the SAME LENGTH right before, not the previous calendar month. The
        // screen default is "from the start of the month until now"; comparing the first 10 days of a month with
        // the whole previous month would always make this month look like it collapsed, and that is a fault of
        // the comparison, not of the business.
        Duration windowLength = Duration.between(from, to);
        Instant previousFrom = from.minus(windowLength);
        BigDecimal previousRevenue = zeroIfNull(orderRepository.sumTotalAmountByStatusInRange(
            OrderStatus.SUCCESS, previousFrom, from));

        // The previous period cost only builds the previous MARGIN, so it is not returned: the screen asks "how many
        // points is this month margin above or below last month", not how much the previous period cost.
        BigDecimal previousAiCost = zeroIfNull(platformBusinessHealthQueryRepository.sumAiCostVnd(previousFrom, from));

        return new PlatformBusinessHealthResponse(
            health.subscribedSchools(), health.expiringSoonSchools(), health.lapsedSchools(),
            health.suspendedSchools(), schoolsInDebt,
            revenue, previousRevenue, aiCost,
            grossMarginPercent(revenue, aiCost),
            grossMarginPercent(previousRevenue, previousAiCost));
    }

    /**
     * {@code null} khi chưa thu được đồng nào: biên lợi nhuận của doanh thu 0 không phải là 0%, và
     * cũng không phải -∞ — nó không tồn tại. Ép thành 0% sẽ vẽ ra một tháng "biên 0%" trông như đang
     * lỗ, trong khi thật ra chỉ là chưa có hóa đơn nào rơi vào cửa sổ.
     */
    private static Double grossMarginPercent(BigDecimal revenue, BigDecimal aiCost) {
        if (revenue.signum() <= 0) {
            return null;
        }
        return revenue.subtract(aiCost)
            .multiply(BigDecimal.valueOf(100))
            .divide(revenue, 1, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
