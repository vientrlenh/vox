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
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SchoolFundingResponse;
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
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
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
    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private ViewSchoolAdminDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        UserContextPort userContextPort = mock(UserContextPort.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        SubscriptionPlanRepository subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        subscriptionQuotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        schoolWorkloadQueryRepository = mock(SchoolWorkloadQueryRepository.class);
        orderRepository = mock(OrderRepository.class);

        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        // Counting submissions by status goes through findAccessible and then takes totalElements
        // -- a mock that returns null would throw an NPE right away, so a real empty page must be
        // returned.
        when(assessmentRepository.findAccessible(any(), any(), anyBoolean(), anyBoolean(), any(), any(), any(),
            any(), any(), anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(assessmentResultAppealRepository.countBySchoolIdAndStatusIn(any(), any())).thenReturn(0L);
        when(assessmentResultAppealRepository.findOldestPendingRequestedAt(SCHOOL_ID)).thenReturn(null);
        when(schoolSubscriptionRepository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(any(), any()))
            .thenReturn(Optional.empty());
        // This port promises NEVER null (COALESCE in the query + clamping again in the adapter), so
        // the default mock must be ZERO and not Mockito's null -- letting null through gives a
        // green test for a behavior production cannot have, and hides every addition/subtraction
        // below.
        when(subscriptionQuotaUserAllocationRepository.sumUnusedAllocation(any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(orderRepository.findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any())).thenReturn(List.of());
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        when(schoolWorkloadQueryRepository.countUnscored(eq(SCHOOL_ID), any()))
            .thenReturn(SchoolUnscoredWorkloadDto.empty());
        when(schoolWorkloadQueryRepository.findAssessmentsAwaitingPublish(eq(SCHOOL_ID), any(), anyInt()))
            .thenReturn(List.of());

        useCase = new ViewSchoolAdminDashboardUseCase(
            userContextPort, assessmentRepository, assessmentResultAppealRepository,
            schoolSubscriptionRepository, subscriptionQuotaRepository,
            subscriptionQuotaUserAllocationRepository,
            subscriptionPlanRepository, orderRepository, schoolBalanceRepository,
            schoolWorkloadQueryRepository);
    }

    @Test
    void should_bucket_successful_orders_into_their_month_and_zero_fill_the_rest() {
        Instant now = Instant.now();
        Instant fiveMonthsAgo = now.minus(150, ChronoUnit.DAYS);

        when(orderRepository.findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any())).thenReturn(List.of(
            order(OrderType.SUBSCRIPTION_REQUEST, OrderStatus.SUCCESS, new BigDecimal("500000"), now),
            order(OrderType.TOPUP, OrderStatus.SUCCESS, new BigDecimal("200000"), fiveMonthsAgo)));

        SchoolAdminDashboardSummaryResponse result = useCase.execute(null);

        assertThat(result.monthlySpending()).hasSize(SPENDING_MONTHS);
        assertThat(monthOf(result.monthlySpending(), month(now)).amount()).isEqualByComparingTo("500000");
        assertThat(monthOf(result.monthlySpending(), month(fiveMonthsAgo)).amount()).isEqualByComparingTo("200000");
        assertThat(result.monthlySpending().stream()
            .filter(m -> !m.month().equals(month(now)) && !m.month().equals(month(fiveMonthsAgo)))
            .toList())
            .allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    /**
     * TOPUP is money going into the self-funded wallet, the other orders are plan purchases -- two
     * separate columns that add up to the total.
     */
    @Test
    void should_split_top_ups_from_subscription_spending() {
        Instant now = Instant.now();

        when(orderRepository.findBySchoolIdAndStatusInRange(
            eq(SCHOOL_ID), eq(OrderStatus.SUCCESS), any(), any())).thenReturn(List.of(
            order(OrderType.SUBSCRIPTION_REQUEST, OrderStatus.SUCCESS, new BigDecimal("500000"), now),
            order(OrderType.TOPUP, OrderStatus.SUCCESS, new BigDecimal("300000"), now)));

        SchoolMonthlySpendingResponse bucket = monthOf(useCase.execute(null).monthlySpending(), month(now));

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
     * A school that has never touched the wallet: an empty wallet and a wallet of 0 VND mean the
     * SAME thing, so it must not lock and must not throw an NPE -- this is the state of every new
     * school, not an edge case.
     */
    @Test
    void should_treat_a_school_that_never_touched_its_wallet_as_zero_and_unlocked() {
        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.balanceVnd()).isEqualTo("0");
        assertThat(funding.locked()).isFalse();
        assertThat(funding.spendableVnd()).isEqualTo("0");
    }

    /**
     * A NEGATIVE wallet is not subtracted from "can still be graded": debt is an amount to pay
     * back, not a negative quota. Adding it in would draw "3 million left" for a school that is
     * hard-locked -- exactly the number that reassures the reader wrongly.
     */
    @Test
    void should_not_let_a_negative_wallet_eat_into_the_remaining_assessment_quota() {
        givenAssessmentQuota("12000000", "9000000");
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(balance(new BigDecimal("-1240000"))));

        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.assessmentQuotaRemainingVnd()).isEqualTo("3000000");
        assertThat(funding.spendableVnd()).isEqualTo("3000000");
        assertThat(funding.locked()).isTrue();
    }

    /**
     * A positive wallet is added: when the quota is used up but the wallet still has money, the
     * school can keep grading.
     */
    @Test
    void should_add_a_positive_wallet_to_the_remaining_assessment_quota() {
        givenAssessmentQuota("12000000", "12000000");
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(balance(new BigDecimal("500000"))));

        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.assessmentQuotaRemainingVnd()).isEqualTo("0");
        assertThat(funding.spendableVnd()).isEqualTo("500000");
        assertThat(funding.locked()).isFalse();
    }

    /**
     * The portion promised to teachers must NOT be subtracted from "can still be graded".
     *
     * <p>{@code spendable} is the locking predicate: it must match the gate that
     * ClassTestTokenQuotaGuardService uses to refuse scheduling an assessment, down to the last dong.
     * Subtracting the personal spending ceilings from it would make the dashboard report that money
     * has run out while the system still happily lets it be spent -- a discrepancy far worse than
     * the old number. The promised portion comes out in a SEPARATE field.
     */
    @Test
    void should_report_what_is_promised_to_teachers_without_shrinking_the_spendable_figure() {
        givenAssessmentQuota("12000000", "2000000");
        givenCommittedToUsers("6000000");

        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.spendableVnd()).isEqualTo("10000000");
        assertThat(funding.committedToUsersVnd()).isEqualTo("6000000");
        assertThat(funding.uncommittedVnd()).isEqualTo("4000000");
    }

    /**
     * When the school distributes more to teachers than what is left, "still free" is NEGATIVE and
     * is not clamped to 0.
     *
     * <p>This state is reachable without anyone doing anything wrong: DistributeQuotaToUsersService
     * caps the total allocation by pool.totalAllocated × ratio, i.e. by the TOTAL and not by the
     * REMAINING part, so distributing everything at the start of the period and then opening a few
     * centralized assessments lands here by itself. Clamping to 0 hides exactly that warning -- its
     * consequence (teachers being refused by the school wallet even though their personal ceiling
     * still has room) shows up nowhere else.
     */
    @Test
    void should_surface_over_commitment_as_a_negative_free_figure_instead_of_clamping_it() {
        givenAssessmentQuota("10000000", "4000000");
        givenCommittedToUsers("10000000");

        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.spendableVnd()).isEqualTo("6000000");
        assertThat(funding.uncommittedVnd()).isEqualTo("-4000000");
    }

    /**
     * The self-funded wallet can absorb the promised portion: a personal ceiling has no pocket of
     * its own, so when the plan quota runs out what a teacher spends keeps flowing into the wallet
     * (chargeOverage never looks at the allocation). "Still free" must therefore be reduced across
     * BOTH pockets, not only the plan quota.
     */
    @Test
    void should_count_a_positive_wallet_as_backing_for_what_was_promised_to_teachers() {
        givenAssessmentQuota("12000000", "12000000");
        givenCommittedToUsers("800000");
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(balance(new BigDecimal("2000000"))));

        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.uncommittedVnd()).isEqualTo("1200000");
    }

    /** A school with no running plan has no period to promise anything in -- 0, not an NPE. */
    @Test
    void should_report_nothing_committed_when_the_school_has_no_active_subscription() {
        SchoolFundingResponse funding = useCase.execute(null).funding();

        assertThat(funding.committedToUsersVnd()).isEqualTo("0");
        assertThat(funding.uncommittedVnd()).isEqualTo("0");
    }

    /**
     * Only asks the ASSESSMENT wallet. The student's speaking-practice spending ceiling lives in the PRACTICE wallet and pays nothing for
     * assessment grading; adding it here would make the "still free" amount of assessments be reduced by an unrelated pocket of money --
     * exactly the "merging two pockets" bug that SchoolFundingResponse exists to avoid.
     */
    @Test
    void should_only_ask_about_the_assessment_wallet_when_summing_what_is_promised() {
        givenAssessmentQuota("12000000", "0");

        useCase.execute(null);

        verify(subscriptionQuotaUserAllocationRepository).sumUnusedAllocation(any(), eq(QuotaType.ASSESSMENT));
        verify(subscriptionQuotaUserAllocationRepository, never())
            .sumUnusedAllocation(any(), eq(QuotaType.PRACTICE));
    }

    /** Hàng đợi khiếu nại sạch phải ra null, KHÔNG phải 0 — 0 nghĩa là có đơn vừa nộp hôm nay. */
    @Test
    void should_report_no_appeal_age_when_the_queue_is_empty() {
        assertThat(useCase.execute(null).oldestPendingAppealDays()).isNull();
    }

    /** Đếm theo NGÀY LỊCH giờ VN: đơn nộp 23:00 hôm qua đọc là "đã sang ngày thứ hai". */
    @Test
    void should_count_appeal_age_in_business_calendar_days() {
        when(assessmentResultAppealRepository.findOldestPendingRequestedAt(SCHOOL_ID))
            .thenReturn(Instant.now().minus(19, ChronoUnit.DAYS));

        assertThat(useCase.execute(null).oldestPendingAppealDays()).isEqualTo(19);
    }

    /**
     * The total card and the per-period card read the SAME point in time. Two separate
     * {@code Instant.now()} calls could place the same submission "within the deadline" in one
     * place and "overdue" in another, and two numbers side by side on the same screen disagreeing
     * by a few milliseconds is the kind of bug nobody can reproduce.
     */
    @Test
    void should_read_both_workload_queries_at_the_same_instant() {
        ArgumentCaptor<Instant> countedAt = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> listedAt = ArgumentCaptor.forClass(Instant.class);

        useCase.execute(null);

        verify(schoolWorkloadQueryRepository).countUnscored(eq(SCHOOL_ID), countedAt.capture());
        verify(schoolWorkloadQueryRepository)
            .findAssessmentsAwaitingPublish(eq(SCHOOL_ID), listedAt.capture(), anyInt());
        assertThat(countedAt.getValue()).isEqualTo(listedAt.getValue());
    }

    private void givenAssessmentQuota(String allocated, String used) {
        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setSchoolId(SCHOOL_ID);
        // status/endDate are not relevant to the money calculation here, but
        // buildSubscriptionRenewal runs in the same execute() call and reads both -- leaving them
        // empty is an NPE, not a safe null.
        subscription.setStatus(SchoolSubscriptionStatus.ACTIVE);
        subscription.setEndDate(Instant.parse("2026-12-31T00:00:00Z"));
        when(schoolSubscriptionRepository.findActiveBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(subscription));

        SchoolSubscriptionQuotaRecord quota = new SchoolSubscriptionQuotaRecord();
        quota.setTotalAllocatedAmountVnd(new BigDecimal(allocated));
        quota.setUsedAmountVnd(new BigDecimal(used));
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(
            eq(subscription.getId()), eq(QuotaType.ASSESSMENT))).thenReturn(Optional.of(quota));
    }

    /**
     * Tổng phần CHƯA TIÊU của các trần chi cá nhân — đúng thứ cổng thật trả về.
     *
     * <p>Cố ý không dựng từng dòng allocation: phép kẹp âm theo TỪNG DÒNG nằm trong câu JPQL, nên nơi
     * chốt nó là test của tầng persistence. Ở đây thứ đang test là use case dùng con số ấy ra sao.
     */
    private void givenCommittedToUsers(String unusedAllocationVnd) {
        when(subscriptionQuotaUserAllocationRepository.sumUnusedAllocation(any(), eq(QuotaType.ASSESSMENT)))
            .thenReturn(new BigDecimal(unusedAllocationVnd));
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
        Order built = new Order();
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
