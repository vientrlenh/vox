package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.application.response.input.dashboard.MonthlyRevenueResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

/**
 * Doanh thu giờ đọc từ ĐƠN HÀNG đã thu tiền (OrderStatus.SUCCESS), không còn từ hóa đơn PAID: hóa đơn
 * chỉ phát cho đơn đã thu đủ nên "lọc hóa đơn PAID" vốn là điều kiện luôn đúng, còn đơn thì mang sẵn
 * trạng thái thu tiền.
 *
 * <p>Cắt tháng theo ZoneConstant.BUSINESS_ZONE chứ không UTC -- cắt theo UTC đẩy 7 tiếng đầu mỗi
 * tháng sang tháng trước, tức doanh thu ngày mùng 1 rơi nhầm kỳ.
 */
class ViewSystemAdminDashboardUseCaseTests {

    private static final int REVENUE_MONTHS = 24;

    private OrderRepository orderRepository;
    private RegisterFormRepository registerFormRepository;
    private ViewSystemAdminDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        var schoolRepository = mock(SchoolRepository.class);
        registerFormRepository = mock(RegisterFormRepository.class);
        orderRepository = mock(OrderRepository.class);
        var roleRepository = mock(RoleRepository.class);
        var userRoleRepository = mock(UserRoleRepository.class);
        var frameworkRepository = mock(FrameworkRepository.class);
        var rubricRepository = mock(RubricRepository.class);

        useCase = new ViewSystemAdminDashboardUseCase(
            schoolRepository, registerFormRepository, orderRepository,
            roleRepository, userRoleRepository, frameworkRepository, rubricRepository);

        when(schoolRepository.countAll()).thenReturn(0L);
        when(schoolRepository.countByIsActiveTrue()).thenReturn(0L);
        when(registerFormRepository.countByStatus(any())).thenReturn(0L);
        when(registerFormRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(registerFormRepository.findOldestCreatedAtByStatus(any())).thenReturn(Optional.empty());
        when(roleRepository.findByCode(any())).thenReturn(Optional.empty());
        when(frameworkRepository.findAllActive(1, 1)).thenReturn(new PageResult<>(List.of(), 1, 1, 0, 0));
        when(rubricRepository.findAllByOwnerType(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageResult<>(List.of(), 1, 1, 0, 0));
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.findByStatusInRange(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void should_bucket_successful_orders_into_24_months_zero_filled() {
        var now = Instant.now();
        var thirteenMonthsAgo = now.minus(395, ChronoUnit.DAYS);

        when(orderRepository.findByStatusInRange(any(), any(), any())).thenReturn(List.of(
            successfulOrder(new BigDecimal("200000"), now),
            successfulOrder(new BigDecimal("300000"), thirteenMonthsAgo)));

        var result = useCase.execute(null);

        assertThat(result.monthlyRevenue()).hasSize(REVENUE_MONTHS);

        var currentMonth = YearMonth.now(ZoneConstant.BUSINESS_ZONE).toString();
        var pastMonth = YearMonth.from(thirteenMonthsAgo.atZone(ZoneConstant.BUSINESS_ZONE)).toString();

        assertThat(amountOf(result.monthlyRevenue(), currentMonth)).isEqualByComparingTo("200000");
        assertThat(amountOf(result.monthlyRevenue(), pastMonth)).isEqualByComparingTo("300000");

        assertThat(result.monthlyRevenue().stream()
            .filter(m -> !m.month().equals(currentMonth) && !m.month().equals(pastMonth))
            .toList())
            .allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    /**
     * Đơn nằm ngoài cửa sổ 24 tháng không được cộng vào đâu cả. computeIfPresent là thứ giữ điều đó:
     * tháng của nó không có sẵn trong map nên khoản tiền bị bỏ qua thay vì tạo ra một cột thứ 25.
     */
    @Test
    void should_ignore_orders_outside_the_24_month_window() {
        when(orderRepository.findByStatusInRange(any(), any(), any()))
            .thenReturn(List.of(successfulOrder(new BigDecimal("999999"), Instant.now().minus(800, ChronoUnit.DAYS))));

        var result = useCase.execute(null);

        assertThat(result.monthlyRevenue()).hasSize(REVENUE_MONTHS);
        assertThat(result.monthlyRevenue()).allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    /** Tổng doanh thu là một phép SUM riêng ở tầng DB, không phải cộng lại danh sách theo tháng. */
    @Test
    void should_read_total_revenue_from_the_repository_sum() {
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any()))
            .thenReturn(new BigDecimal("12345678"));

        assertThat(useCase.execute(null).totalRevenue()).isEqualByComparingTo("12345678");
    }

    /**
     * Hàng đợi rỗng phải là null, không phải 0: ở chỉ số này 0 là trạng thái TỐT NHẤT ("có đơn, vừa
     * nộp hôm nay"), nên trả 0 cho hàng đợi rỗng khiến hai tình huống ngược nhau in ra cùng một dòng.
     */
    @Test
    void should_report_no_oldest_pending_day_count_when_the_queue_is_empty() {
        when(registerFormRepository.findOldestCreatedAtByStatus(any())).thenReturn(Optional.empty());

        assertThat(useCase.execute(null).oldestPendingRegistrationDays()).isNull();
    }

    /** Đếm theo NGÀY LỊCH giờ nghiệp vụ, không phải elapsed chia 24 giờ. */
    @Test
    void should_count_oldest_pending_registration_in_business_zone_calendar_days() {
        var sixDaysAgo = LocalDate.now(ZoneConstant.BUSINESS_ZONE)
            .minusDays(6)
            .atTime(9, 30)
            .atZone(ZoneConstant.BUSINESS_ZONE)
            .toInstant();
        when(registerFormRepository.findOldestCreatedAtByStatus(any())).thenReturn(Optional.of(sixDaysAgo));

        assertThat(useCase.execute(null).oldestPendingRegistrationDays()).isEqualTo(6);
    }

    /**
     * Đơn nộp cuối ngày hôm qua đã sang ngày thứ hai của hàng đợi, dù mới trôi qua vài tiếng — phép
     * chia cho 24 giờ sẽ trả 0 và làm thẻ KPI trông như hàng đợi vẫn sạch.
     */
    @Test
    void should_count_yesterday_late_night_submission_as_one_day() {
        var lateYesterday = LocalDate.now(ZoneConstant.BUSINESS_ZONE)
            .minusDays(1)
            .atTime(23, 50)
            .atZone(ZoneConstant.BUSINESS_ZONE)
            .toInstant();
        when(registerFormRepository.findOldestCreatedAtByStatus(any())).thenReturn(Optional.of(lateYesterday));

        assertThat(useCase.execute(null).oldestPendingRegistrationDays()).isEqualTo(1);
    }

    private static BigDecimal amountOf(List<MonthlyRevenueResponse> monthly, String month) {
        return monthly.stream()
            .filter(m -> m.month().equals(month))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Không có cột doanh thu cho tháng " + month))
            .amount();
    }

    private Order successfulOrder(BigDecimal totalVnd, Instant createdAt) {
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setSchoolId(UUID.randomUUID());
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.SUCCESS);
        order.setTotalAmountVnd(totalVnd);
        order.setSubtotalAmountVnd(totalVnd);
        order.setCreatedAt(createdAt);
        return order;
    }
}
