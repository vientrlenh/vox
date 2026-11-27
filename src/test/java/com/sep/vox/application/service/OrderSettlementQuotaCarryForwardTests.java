package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * What SURVIVES the boundary between two subscription periods -- unspent self-funded money, and personal spending caps.
 *
 * <p>This is where V12 is most fragile, and also the ONLY file of that change that had no test yet.
 * {@code seedQuotaRecords} REBUILDS the quota records each period, so everything not deliberately copied over
 * disappears -- last time it was the spending caps (Q-2, "can be redistributed"), this time it is the school's REAL MONEY
 * (Q-7, "cannot be redistributed").
 *
 * <p><b>Some of the tests below ARE RED and that is deliberate.</b> They describe the correct behaviour for the four bugs
 * still open, not the current behaviour; read each test's Javadoc to see which bug. The GREEN tests interleaved
 * between them pin what already works, so the coming fix does not trample it.
 *
 * <p>Because there are red tests, the branch carrying this file must NOT be {@code default}: CI Build is the gate of cd.yml,
 * and red there blocks every other deploy.
 */
class OrderSettlementQuotaCarryForwardTests {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private InvoiceRepository invoiceRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository;
    private SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private SchoolUserRepository schoolUserRepository;
    private RoleRepository roleRepository;
    private OrderSettlementService service;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID previousSubscriptionId = UUID.randomUUID();
    private final UUID newSubscriptionId = UUID.randomUUID();

    private final Instant now = Instant.parse("2026-11-01T03:00:00Z");

    /** Kept so the test can read it back -- the old period is NOT modified by settlement, and that is exactly the problem in the early-renewal case. */
    private final Map<QuotaType, SchoolSubscriptionQuotaRecord> previousPools = new EnumMap<>(QuotaType.class);

    private SchoolSubscription previousSubscription;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        quotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        subscriptionPlanQuotaRepository = mock(SubscriptionPlanQuotaRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        roleRepository = mock(RoleRepository.class);
        SubscriptionPlanRepository subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);

        service = new OrderSettlementService(
            orderRepository,
            orderItemRepository,
            mock(PaymentRecordRepository.class),
            invoiceRepository,
            mock(SchoolBalanceRepository.class),
            mock(SchoolBalanceEntryRepository.class),
            schoolSubscriptionRepository,
            quotaRecordRepository,
            quotaUserAllocationRepository,
            subscriptionPlanRepository,
            subscriptionPlanQuotaRepository,
            mock(SchoolDebtNotificationService.class),
            schoolUserRepository,
            roleRepository,
            mock(OutboxRepository.class),
            mock(JsonSerializationPort.class));

        Order order = new Order();
        order.setId(orderId);
        order.setSchoolId(schoolId);
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotalAmountVnd(BigDecimal.valueOf(20_000_000));
        order.setTotalAmountVnd(BigDecimal.valueOf(20_000_000));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setType(OrderItemType.SUBSCRIPTION);
        item.setItemId(planId);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(planId);
        plan.setName("Gói năm");
        plan.setPeriodType(SubscriptionPlanPeriod.YEAR);
        plan.setPeriodCount(1);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        // The invoice is treated as already issued: that branch (and the outbox that follows) has
        // nothing to do with carrying quota over into the new period, so it is cut off to make the
        // test say exactly one thing.
        when(invoiceRepository.existsByOrderId(orderId)).thenReturn(true);

        // Only assign an id to the NEW row. cutOverToUpgrade also calls save() on the old periods,
        // and overwriting their ids here would make two periods carry the same id.
        when(schoolSubscriptionRepository.save(any())).thenAnswer(invocation -> {
            SchoolSubscription saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(newSubscriptionId);
            }
            return saved;
        });

        previousSubscription = new SchoolSubscription();
        previousSubscription.setId(previousSubscriptionId);
        previousSubscription.setSchoolId(schoolId);
        previousSubscription.setStatus(SchoolSubscriptionStatus.ACTIVE);
        previousSubscription.setEndDate(now.minus(1, ChronoUnit.DAYS));
        when(schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId))
            .thenReturn(Optional.of(previousSubscription));

        // Default: the old period has expired, so the new period runs IMMEDIATELY -- the cut-off
        // point coincides with the start point.
        when(schoolSubscriptionRepository.findUnfinishedBySchoolId(eq(schoolId), any()))
            .thenReturn(List.of());

        givenNewPlanQuotas(Map.of(QuotaType.ASSESSMENT, 10_000_000L, QuotaType.PRACTICE, 10_000_000L));
    }

    // =====================================================================
    // GREEN -- behaviour already correct, pinned so the next fix does not trample it
    // =====================================================================

    @Test
    void renewal_after_expiry_carries_unspent_funding_into_the_new_period() {
        // Q-7. This money is NO LONGER in school_balances (it left the wallet together with a
        // QUOTA_FUNDING entry); it sits right in the record being rebuilt -- if it is not carried
        // over, real money is wiped on the very day of renewal.
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        SchoolSubscriptionQuotaRecord seeded = seededPool(QuotaType.PRACTICE);
        assertThat(seeded.getFundedFromBalanceVnd()).isEqualByComparingTo("5000000");
        assertThat(seeded.getTotalAllocatedAmountVnd()).isEqualByComparingTo("15000000");
        assertThat(seeded.getUsedAmountVnd()).isEqualByComparingTo("0");
    }

    @Test
    void only_the_unspent_part_of_the_funding_is_carried() {
        // Convention "plan money is spent first, top-up money after": spending 12m from a 15m
        // wallet that has 5m of top-up leaves only 3m to carry over; the other 2m was really spent.
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 12_000_000, 5_000_000);

        settle();

        SchoolSubscriptionQuotaRecord seeded = seededPool(QuotaType.PRACTICE);
        assertThat(seeded.getFundedFromBalanceVnd()).isEqualByComparingTo("3000000");
        assertThat(seeded.getTotalAllocatedAmountVnd()).isEqualByComparingTo("13000000");
    }

    @Test
    void renewal_carries_personal_ceilings_so_the_school_does_not_stall() {
        // Q-2. If it is not copied, PRACTICE drops to 0 for the WHOLE SCHOOL (LEAST + COALESCE),
        // while for ASSESSMENT every teacher ends up with no ceiling -- two failures in opposite
        // directions, equally silent.
        UUID teacher = UUID.randomUUID();
        givenPreviousAllocations(QuotaType.ASSESSMENT, Map.of(teacher, 2_000_000L));

        settle();

        verify(quotaUserAllocationRepository).upsertAllocation(
            eq(newSubscriptionId), eq(QuotaType.ASSESSMENT), eq(teacher), eq(BigDecimal.valueOf(2_000_000)));
    }

    @Test
    void a_first_ever_subscription_carries_nothing() {
        when(schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId)).thenReturn(Optional.empty());

        settle();

        assertThat(seededPool(QuotaType.PRACTICE).getFundedFromBalanceVnd()).isEqualByComparingTo("0");
        verify(quotaUserAllocationRepository, never()).upsertAllocation(any(), any(), any(), any());
    }

    // =====================================================================
    // RED -- four bugs still open. See each test's Javadoc.
    // =====================================================================

    /**
     * RED -- early renewal, the "money doubles" direction.
     *
     * <p>{@code seedQuotaRecords} snapshots the unspent portion at the moment of PAYMENT, but
     * {@code nextPeriodStart} makes the new period start at the old period's endDate. In the gap in
     * between, the old period is still the one {@code findActiveBySchoolId} returns and its quota
     * wallet can still be spent -- so those 5m sit both in the old period (spendable until 31/12)
     * and already copied into the new period (spendable again from 1/1).
     *
     * <p>An invariant is broken, and it does not depend on how it is fixed: the total OPEN top-up
     * money across a school's periods must not exceed what they actually topped up.
     */
    @Test
    void early_renewal_must_not_leave_the_same_funded_money_open_in_two_periods() {
        givenPreviousPeriodStillInForce();
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        BigDecimal stillOpenOnOldPeriod = previousPools.get(QuotaType.PRACTICE).getFundedFromBalanceVnd();
        BigDecimal carriedToNewPeriod = seededPool(QuotaType.PRACTICE).getFundedFromBalanceVnd();

        assertThat(stillOpenOnOldPeriod.add(carriedToNewPeriod))
            .as("5M self-funded open in both periods at the same time")
            .isEqualByComparingTo("5000000");
    }

    /**
     * RED -- early renewal, the "money evaporates" direction.
     *
     * <p>The other face of the same wrong milestone. The school pays for the November renewal and then tops up 3M in December:
     * that amount lands in the quota record of the OLD PERIOD (the only one in force), is not in the snapshot taken
     * in November, and dies with the old period at the boundary. Exactly the Q-7 bug, narrowed to
     * the early-renewal window.
     *
     * <p>It cannot be patched by taking a better snapshot: every figure computed BEFORE the old period stops spending can
     * be made wrong by the old period itself. The carry-over must run at the BOUNDARY. This test only demands
     * one thing -- do not fix that figure at payment time -- and leaves open whether to choose a sweeping job or to
     * materialize on read.
     *
     * <p>A renewal after the old period has expired makes the two milestones coincide, and
     * {@link #renewal_after_expiry_carries_unspent_funding_into_the_new_period} keeps the requirement to
     * carry over IMMEDIATELY in that case.
     */
    @Test
    void funding_is_carried_at_the_period_boundary_not_at_settlement() {
        givenPreviousPeriodStillInForce();
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        verify(quotaRecordRepository, never()).findBySchoolSubscriptionId(previousSubscriptionId);
    }

    /**
     * Deferring without leaving a trace is also losing money, just by a different path. The future period must carry
     * a MARKER pointing back to the source period -- that is everything connecting the two halves of the carry-over, and
     * {@code CarryQuotaFundingAtPeriodStartService} has no other way to find the source period.
     */
    @Test
    void an_early_renewal_leaves_a_promise_pointing_back_at_the_period_it_must_draw_from() {
        givenPreviousPeriodStillInForce();
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        assertThat(seededPool(QuotaType.PRACTICE).getCarryFundingFromSubscriptionId())
            .isEqualTo(previousSubscriptionId);
    }

    @Test
    void a_renewal_that_starts_immediately_leaves_no_promise_behind() {
        // The closing milestone coincides with the start: already carried over on the spot, and a lingering marker would be the job adding a second time.
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        assertThat(seededPool(QuotaType.PRACTICE).getCarryFundingFromSubscriptionId()).isNull();
    }

    /**
     * RED -- the allocation row of a person who has left the school is copied over, forever.
     *
     * <p>Q-5 decided that these rows do NOT count toward the ceiling
     * ({@code sumAllocatedForEligibleUsers}), but {@code carryForwardUserAllocations} still reads
     * ALL rows of the old period. There is no way to delete them, so every renewal rebuilds exactly
     * that set plus whoever left recently -- the table grows with the entire staffing history, and
     * the {@code orphanedAmountVnd} that the screen shows the school can only go up.
     *
     * <p>The filter must be EXACTLY the one {@code sumAllocatedForEligibleUsers} uses -- ACTIVE +
     * still belonging to the school + the right role. {@code findBySchoolIdWithRole} looks suitable
     * but filters {@code status <> DISABLED}, so it still accepts INACTIVE and LOCKED: looser than
     * the ceiling, and the two sides counting different sets is exactly the Q-5 bug.
     */
    @Test
    void allocations_of_users_who_have_left_the_school_are_not_carried_forward() {
        UUID stillHere = UUID.randomUUID();
        UUID departed = UUID.randomUUID();
        givenPreviousAllocations(QuotaType.ASSESSMENT, Map.of(stillHere, 2_000_000L, departed, 3_000_000L));
        givenEligible(SchoolRoleCodes.TEACHER, stillHere);

        settle();

        verify(quotaUserAllocationRepository, never()).upsertAllocation(
            eq(newSubscriptionId), eq(QuotaType.ASSESSMENT), eq(departed), any());
    }

    /**
     * RED -- the carried personal cap is not clamped to the NEW period's quota.
     *
     * <p>A smaller new plan (downgrade, or promotion ending) can make the total of the copied caps larger than the quota.
     * The school opens the new period already over its own cap: the amber banner is on from the start, and every
     * manual edit afterwards is rejected with "exceeds the distributable share" -- exactly the Q-6 symptom, re-entered through the renewal door
     * instead of the even-split door.
     *
     * <p>The test clamps at a level that is certainly correct -- never copy more than the quota that exists. The real cap is
     * tighter still ({@code distributableAmountOf}, by the school's ratio), but that policy sits in a
     * repository this service does not hold yet, so it is left for whoever fixes it to choose.
     */
    @Test
    void carried_allocations_are_clamped_to_the_new_periods_pool() {
        givenNewPlanQuotas(Map.of(QuotaType.ASSESSMENT, 10_000_000L));
        givenPreviousAllocations(QuotaType.ASSESSMENT,
            Map.of(UUID.randomUUID(), 9_000_000L, UUID.randomUUID(), 9_000_000L));

        settle();

        assertThat(carriedAmounts(QuotaType.ASSESSMENT).stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b)))
            .as("18M of personal cap copied into a quota record holding only 10M")
            .isLessThanOrEqualTo(BigDecimal.valueOf(10_000_000));
    }

    /**
     * RED -- copying caps for a quota type that the new plan NO LONGER has.
     *
     * <p>{@code carryForwardUserAllocations} walks {@code QuotaType.values()} and upserts unconditionally,
     * without asking whether the new period has a quota record of that type. A school moving to a plan without PRACTICE would end up with a batch of
     * allocation rows pointing at a quota record that does not exist.
     *
     * <p>{@code seedQuotaRecords} already handles the symmetric case for MONEY -- it writes a WARN with the amount that could not
     * be carried. The cap side is silent. The list of quota types already built is available there (seededTypes) and
     * {@code seedQuotaRecords} already runs first, so it is just a matter of passing it along.
     */
    @Test
    void allocations_are_not_carried_for_quota_types_the_new_plan_dropped() {
        givenNewPlanQuotas(Map.of(QuotaType.ASSESSMENT, 10_000_000L));
        givenPreviousAllocations(QuotaType.PRACTICE, Map.of(UUID.randomUUID(), 1_000_000L));

        settle();

        verify(quotaUserAllocationRepository, never()).upsertAllocation(
            any(), eq(QuotaType.PRACTICE), any(), any());
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private void settle() {
        PaymentRecord payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setAmountVnd(BigDecimal.valueOf(20_000_000));
        service.settlePaid(payment, now);
    }

    /** EARLY renewal: the old period is still valid, so the new period queues behind it instead of running immediately. */
    private void givenPreviousPeriodStillInForce() {
        previousSubscription.setEndDate(now.plus(60, ChronoUnit.DAYS));
        when(schoolSubscriptionRepository.findUnfinishedBySchoolId(eq(schoolId), any()))
            .thenReturn(List.of(previousSubscription));
    }

    private void givenNewPlanQuotas(Map<QuotaType, Long> includedByType) {
        List<SubscriptionPlanQuota> quotas = includedByType.entrySet().stream()
            .map(entry -> SubscriptionPlanQuota.builder()
                .subscriptionPlanId(planId)
                .quotaType(entry.getKey())
                .includedAmountVnd(BigDecimal.valueOf(entry.getValue()))
                .build())
            .toList();
        when(subscriptionPlanQuotaRepository.findBySubscriptionPlanId(planId)).thenReturn(quotas);
    }

    private void givenPreviousPool(QuotaType quotaType, long totalVnd, long usedVnd, long fundedVnd) {
        previousPools.put(quotaType, SchoolSubscriptionQuotaRecord.builder()
            .id(UUID.randomUUID())
            .schoolSubscriptionId(previousSubscriptionId)
            .quotaType(quotaType)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(totalVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .fundedFromBalanceVnd(BigDecimal.valueOf(fundedVnd))
            .build());
        when(quotaRecordRepository.findBySchoolSubscriptionId(previousSubscriptionId))
            .thenReturn(new ArrayList<>(previousPools.values()));
    }

    private void givenPreviousAllocations(QuotaType quotaType, Map<UUID, Long> allocatedByUser) {
        List<SchoolSubscriptionQuotaUserAllocation> rows = allocatedByUser.entrySet().stream()
            .map(entry -> SchoolSubscriptionQuotaUserAllocation.builder()
                .schoolSubscriptionId(previousSubscriptionId)
                .quotaType(quotaType)
                .userId(entry.getKey())
                .allocatedAmountVnd(BigDecimal.valueOf(entry.getValue()))
                .usedAmountVnd(BigDecimal.ZERO)
                .build())
            .toList();
        when(quotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaType(previousSubscriptionId, quotaType))
            .thenReturn(rows);
    }

    /**
     * The people who are STILL eligible for this role's ceiling. If the helper is not called,
     * roleRepository returns an empty Optional and the service copies EVERYTHING -- exactly the
     * fail-open behavior that was chosen, so the other tests here do not need to know anything
     * about roles.
     */
    private void givenEligible(String roleCode, UUID... userIds) {
        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setId(roleId);
        when(roleRepository.findByCode(roleCode)).thenReturn(Optional.of(role));

        List<SchoolUser> members = Arrays.stream(userIds).map(this::schoolUser).toList();
        when(schoolUserRepository.findBySchoolId(
                eq(schoolId), any(), eq(roleId), eq(UserStatus.ACTIVE.name()), any(), eq(false), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(members, 1, members.size(), members.size(), 1));
    }

    private SchoolUser schoolUser(UUID userId) {
        SchoolUser schoolUser = new SchoolUser();
        schoolUser.setUserId(userId);
        return schoolUser;
    }

    private SchoolSubscriptionQuotaRecord seededPool(QuotaType quotaType) {
        ArgumentCaptor<SchoolSubscriptionQuotaRecord> captor = ArgumentCaptor.forClass(SchoolSubscriptionQuotaRecord.class);
        verify(quotaRecordRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
            .filter(record -> record.getQuotaType() == quotaType)
            .findFirst()
            .orElseThrow(() -> new AssertionError("The new period has no " + quotaType + " quota record built"));
    }

    private List<BigDecimal> carriedAmounts(QuotaType quotaType) {
        ArgumentCaptor<BigDecimal> captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(quotaUserAllocationRepository, atLeastOnce()).upsertAllocation(
            eq(newSubscriptionId), eq(quotaType), any(), captor.capture());
        return captor.getAllValues();
    }
}
