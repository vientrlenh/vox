package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolMonthlySpendingResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
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
    private ExamResultAppealRepository examResultAppealRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private ViewSchoolAdminDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        var userContextPort = mock(UserContextPort.class);
        var examRepository = mock(ExamRepository.class);
        var subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        schoolWorkloadQueryRepository = mock(SchoolWorkloadQueryRepository.class);
        orderRepository = mock(OrderRepository.class);

        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        // Đếm bài theo trạng thái đi qua findAccessible rồi lấy totalElements -- mock trả null thì
        // NPE ngay, nên phải trả về một trang rỗng thật.
        when(examRepository.findAccessible(any(), any(), anyBoolean(), anyBoolean(), any(), any(), any(),
            any(), any(), anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examResultAppealRepository.countBySchoolIdAndStatusIn(any(), any())).thenReturn(0L);
        when(examResultAppealRepository.findOldestPendingRequestedAt(SCHOOL_ID)).thenReturn(null);
        when(schoolSubscriptionRepository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(any(), any()))
            .thenReturn(Optional.empty());
        when(orderRepository.findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any())).thenReturn(List.of());
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolWorkloadQueryRepository.countUnscored(eq(SCHOOL_ID), any()))
            .thenReturn(SchoolUnscoredWorkloadDto.empty());
        when(schoolWorkloadQueryRepository.findExamsAwaitingPublish(eq(SCHOOL_ID), any(), anyInt()))
            .thenReturn(List.of());

        useCase = new ViewSchoolAdminDashboardUseCase(
            userContextPort, examRepository, examResultAppealRepository,
            schoolSubscriptionRepository, subscriptionQuotaRepository,
            subscriptionPlanRepository, orderRepository, schoolBalanceRepository,
            schoolWorkloadQueryRepository);
    }

    @Test
    void should_bucket_successful_orders_into_their_month_and_zero_fill_the_rest() {
        var now = Instant.now();
        var fiveMonthsAgo = now.minus(150, ChronoUnit.DAYS);

        when(orderRepository.findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any())).thenReturn(List.of(
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

        when(orderRepository.findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any())).thenReturn(List.of(
            order(OrderType.SUBSCRIPTION_REQUEST, OrderStatus.SUCCESS, new BigDecimal("500000"), now),
            order(OrderType.TOPUP, OrderStatus.SUCCESS, new BigDecimal("300000"), now)));

        var bucket = monthOf(useCase.execute(null).monthlySpending(), month(now));

        assertThat(bucket.subscriptionAmount()).isEqualByComparingTo("500000");
        assertThat(bucket.tokenTopUpAmount()).isEqualByComparingTo("300000");
        assertThat(bucket.amount())
            .isEqualByComparingTo(bucket.subscriptionAmount().add(bucket.tokenTopUpAmount()));
    }

    /**
     * Đơn chưa thu được tiền không phải là chi tiêu -- nhưng phép lọc đó giờ nằm trong CÂU TRUY VẤN
     * chứ không còn lọc lại ở Java. Bản cũ của test này giả lập repository trả về đơn PENDING/FAILED
     * rồi đòi use case bỏ qua chúng; với truy vấn đã lọc sẵn theo status thì tình huống đó không tồn
     * tại, nên thứ đáng chốt là use case HỎI repository đúng cái gì.
     *
     * <p>Vế schoolId không thừa: {@code findByStatusInRange} (bản KHÔNG lọc trường, dành cho màn
     * System Admin) vẫn còn đó, và gọi nhầm nó ở đây là đưa doanh thu của mọi trường vào tổng chi và
     * biểu đồ của đúng một trường -- không test nào khác trong lớp này bắt được, vì mock nào cũng trả
     * về đúng thứ mình dựng sẵn.
     */
    @Test
    void should_only_ask_for_this_schools_successful_orders() {
        useCase.execute(null);

        verify(orderRepository).findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any());
        verify(orderRepository, never()).findByStatusInRange(any(), any(), any());
    }

    /**
     * Trường chưa từng chạm vào ví: ví rỗng và ví 0 đồng là CÙNG một nghĩa, nên không khoá và không
     * được ném NPE — đây là trạng thái của mọi trường mới, không phải ca biên.
     */
    @Test
    void should_treat_a_school_that_never_touched_its_wallet_as_zero_and_unlocked() {
        var funding = useCase.execute(null).funding();

        assertThat(funding.balanceVnd()).isEqualTo("0");
        assertThat(funding.locked()).isFalse();
        assertThat(funding.spendableVnd()).isEqualTo("0");
    }

    /**
     * Ví ÂM không bị trừ vào "còn chấm được": nợ là khoản phải trả, không phải hạn mức âm. Cộng nó
     * vào sẽ vẽ ra "còn 3 triệu" cho một trường đang bị khoá cứng, tức đúng con số khiến người đọc
     * yên tâm sai.
     */
    @Test
    void should_not_let_a_negative_wallet_eat_into_the_remaining_exam_quota() {
        givenExamQuota("12000000", "9000000");
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(balance(new BigDecimal("-1240000"))));

        var funding = useCase.execute(null).funding();

        assertThat(funding.examQuotaRemainingVnd()).isEqualTo("3000000");
        assertThat(funding.spendableVnd()).isEqualTo("3000000");
        assertThat(funding.locked()).isTrue();
    }

    /** Ví dương thì cộng vào: hạn mức cạn mà ví còn tiền là trường vẫn chấm tiếp được. */
    @Test
    void should_add_a_positive_wallet_to_the_remaining_exam_quota() {
        givenExamQuota("12000000", "12000000");
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(balance(new BigDecimal("500000"))));

        var funding = useCase.execute(null).funding();

        assertThat(funding.examQuotaRemainingVnd()).isEqualTo("0");
        assertThat(funding.spendableVnd()).isEqualTo("500000");
        assertThat(funding.locked()).isFalse();
    }

    /** Hàng đợi khiếu nại sạch phải ra null, KHÔNG phải 0 — 0 nghĩa là có đơn vừa nộp hôm nay. */
    @Test
    void should_report_no_appeal_age_when_the_queue_is_empty() {
        assertThat(useCase.execute(null).oldestPendingAppealDays()).isNull();
    }

    /** Đếm theo NGÀY LỊCH giờ VN: đơn nộp 23:00 hôm qua đọc là "đã sang ngày thứ hai". */
    @Test
    void should_count_appeal_age_in_business_calendar_days() {
        when(examResultAppealRepository.findOldestPendingRequestedAt(SCHOOL_ID))
            .thenReturn(Instant.now().minus(19, ChronoUnit.DAYS));

        assertThat(useCase.execute(null).oldestPendingAppealDays()).isEqualTo(19);
    }

    /**
     * Thẻ tổng và thẻ theo kỳ đọc CÙNG một mốc thời gian. Hai lần {@code Instant.now()} riêng có thể
     * xếp cùng một bài vào "đang trong hạn" ở chỗ này và "quá hạn" ở chỗ kia, và hai con số cạnh
     * nhau trên cùng màn hình lệch nhau vì vài mili giây là loại lỗi không ai dựng lại được.
     */
    @Test
    void should_read_both_workload_queries_at_the_same_instant() {
        var countedAt = ArgumentCaptor.forClass(Instant.class);
        var listedAt = ArgumentCaptor.forClass(Instant.class);

        useCase.execute(null);

        verify(schoolWorkloadQueryRepository).countUnscored(eq(SCHOOL_ID), countedAt.capture());
        verify(schoolWorkloadQueryRepository)
            .findExamsAwaitingPublish(eq(SCHOOL_ID), listedAt.capture(), anyInt());
        assertThat(countedAt.getValue()).isEqualTo(listedAt.getValue());
    }

    private void givenExamQuota(String allocated, String used) {
        var subscription = new SchoolSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setSchoolId(SCHOOL_ID);
        // status/endDate không liên quan tới phép tính tiền ở đây, nhưng buildSubscriptionRenewal
        // chạy trong cùng một lượt execute() và đọc cả hai — bỏ trống là NPE, không phải null an toàn.
        subscription.setStatus(SchoolSubscriptionStatus.ACTIVE);
        subscription.setEndDate(Instant.parse("2026-12-31T00:00:00Z"));
        when(schoolSubscriptionRepository.findActiveBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(subscription));

        var quota = new SchoolSubscriptionQuotaRecord();
        quota.setTotalAllocatedAmountVnd(new BigDecimal(allocated));
        quota.setUsedAmountVnd(new BigDecimal(used));
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(
            eq(subscription.getId()), eq(QuotaType.EXAM))).thenReturn(Optional.of(quota));
    }

    private static SchoolBalance balance(BigDecimal balanceVnd) {
        return new SchoolBalance(SCHOOL_ID, balanceVnd, Instant.now(), Instant.now());
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
