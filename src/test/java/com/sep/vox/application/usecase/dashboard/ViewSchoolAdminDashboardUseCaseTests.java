package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.dashboard.SchoolMonthlySpendingResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Chi tiêu của trường đọc từ ĐƠN HÀNG đã thu tiền (OrderStatus.SUCCESS), không còn từ hóa đơn PAID.
 *
 * <p>Điểm mới đáng test là tách hai loại tiền: đơn TOPUP là nạp vào ví tự nạp, còn lại là mua/gia hạn
 * gói. Trên cùng một màn hình, tổng phải bằng đúng tổng hai cột đó -- nếu không thì người đọc không
 * cách nào đối chiếu được con số nào với con số nào.
 */
class ViewSchoolAdminDashboardUseCaseTests {

    private static final int SPENDING_MONTHS = 12;
    private static final UUID SCHOOL_ID = UUID.randomUUID();

    private OrderRepository orderRepository;
    private ViewSchoolAdminDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        var userContextPort = mock(UserContextPort.class);
        var examRepository = mock(ExamRepository.class);
        var examResultAppealRepository = mock(ExamResultAppealRepository.class);
        var schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        var subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        var subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        orderRepository = mock(OrderRepository.class);

        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        // Đếm bài theo trạng thái đi qua findAccessible rồi lấy totalElements -- mock trả null thì
        // NPE ngay, nên phải trả về một trang rỗng thật.
        when(examRepository.findAccessible(any(), any(), anyBoolean(), anyBoolean(), any(), any(), any(),
            any(), any(), anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examResultAppealRepository.countBySchoolIdAndStatusIn(any(), any())).thenReturn(0L);
        when(schoolSubscriptionRepository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(any(), any()))
            .thenReturn(Optional.empty());
        when(orderRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of());

        useCase = new ViewSchoolAdminDashboardUseCase(
            userContextPort, examRepository, examResultAppealRepository,
            schoolSubscriptionRepository, subscriptionQuotaRepository,
            subscriptionPlanRepository, orderRepository);
    }

    @Test
    void should_bucket_successful_orders_into_their_month_and_zero_fill_the_rest() {
        var now = Instant.now();
        var fiveMonthsAgo = now.minus(150, ChronoUnit.DAYS);

        when(orderRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of(
            order(OrderType.SUBSCRIPTION_REQUEST, OrderStatus.SUCCESS, new BigDecimal("500000"), now),
            order(OrderType.TOPUP, OrderStatus.SUCCESS, new BigDecimal("200000"), fiveMonthsAgo)));

        var result = useCase.execute(null);

        assertThat(result.monthlySpending()).hasSize(SPENDING_MONTHS);
        assertThat(monthOf(result.monthlySpending(), month(now)).amount()).isEqualByComparingTo("500000");
        assertThat(monthOf(result.monthlySpending(), month(fiveMonthsAgo)).amount()).isEqualByComparingTo("200000");
        assertThat(result.monthlySpending().stream()
            .filter(m -> !m.month().equals(month(now)) && !m.month().equals(month(fiveMonthsAgo)))
            .toList())
            .allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    /** TOPUP là tiền vào ví tự nạp, đơn còn lại là tiền mua gói -- hai cột riêng, cộng lại bằng tổng. */
    @Test
    void should_split_top_ups_from_subscription_spending() {
        var now = Instant.now();

        when(orderRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of(
            order(OrderType.SUBSCRIPTION_REQUEST, OrderStatus.SUCCESS, new BigDecimal("500000"), now),
            order(OrderType.TOPUP, OrderStatus.SUCCESS, new BigDecimal("300000"), now)));

        var bucket = monthOf(useCase.execute(null).monthlySpending(), month(now));

        assertThat(bucket.subscriptionAmount()).isEqualByComparingTo("500000");
        assertThat(bucket.tokenTopUpAmount()).isEqualByComparingTo("300000");
        assertThat(bucket.amount())
            .isEqualByComparingTo(bucket.subscriptionAmount().add(bucket.tokenTopUpAmount()));
    }

    /** Đơn chưa thu được tiền không phải là chi tiêu -- chỉ SUCCESS mới được tính. */
    @Test
    void should_ignore_orders_that_never_completed() {
        var now = Instant.now();

        when(orderRepository.findBySchoolId(SCHOOL_ID)).thenReturn(List.of(
            order(OrderType.SUBSCRIPTION_REQUEST, OrderStatus.PENDING, new BigDecimal("500000"), now),
            order(OrderType.TOPUP, OrderStatus.FAILED, new BigDecimal("300000"), now)));

        var result = useCase.execute(null);

        assertThat(result.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.monthlySpending())
            .allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    private static String month(Instant at) {
        return YearMonth.from(at.atZone(ZoneConstant.BUSINESS_ZONE)).toString();
    }

    private static SchoolMonthlySpendingResponse monthOf(
            List<SchoolMonthlySpendingResponse> monthly, String month) {
        return monthly.stream()
            .filter(m -> m.month().equals(month))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Không có cột chi tiêu cho tháng " + month));
    }

    private Order order(OrderType type, OrderStatus status, BigDecimal totalVnd, Instant createdAt) {
        var built = new Order();
        built.setId(UUID.randomUUID());
        built.setSchoolId(SCHOOL_ID);
        built.setType(type);
        built.setStatus(status);
        built.setTotalAmountVnd(totalVnd);
        built.setSubtotalAmountVnd(totalVnd);
        built.setCreatedAt(createdAt);
        return built;
    }
}
