package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.command.subscription.AllocateUserQuotaAmountCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationPageResponse;
import com.sep.vox.domain.common.DistributionMode;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Splitting the personal quota -- where the school admin turns the school-level balance into a spending cap per person.
 *
 * <p>The four invariants this suite holds, all four were once wrong and all four broke silently:
 *
 * <ul>
 *   <li><b>Only people who are STILL eligible take up room in the cap.</b> Allocation rows are not deleted when a
 *       teacher leaves, while the table only shows people currently employed -- counting those invisible rows
 *       lets them eat into the cap until every even split is rejected, with an error message that matches
 *       no number on the screen.</li>
 *   <li><b>The automatic even split must match EXACTLY the distributable amount.</b> Not over (exceeding the school's own cap
 *       right after one click) and not under (losing money to rounding).</li>
 *   <li><b>"Not distributed" differs from "distributed 0 dong".</b> The two states behave oppositely at the blocking gate, so
 *       the read layer must be able to tell them apart instead of returning the same 0.</li>
 *   <li><b>The distribution cap is NOT the ability to pay.</b> The cap is computed on the total plan quota and does not
 *       shrink when the balance is spent; the amount that can really still be paid is a SEPARATE number, and the screen must show
 *       both.</li>
 * </ul>
 */
class DistributeQuotaToUsersServiceTests {

    private UserContextPort userContextPort;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository allocationRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private DistributeQuotaToUsersService service;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final UUID teacherA = UUID.randomUUID();
    private final UUID teacherB = UUID.randomUUID();
    /** Has left: the allocation row is still in the DB but no longer on any page of the table. */
    private final UUID formerTeacher = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        allocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolQuotaPolicyRepository = mock(SchoolQuotaPolicyRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);

        service = new DistributeQuotaToUsersService(
            userContextPort,
            schoolSubscriptionRepository,
            subscriptionQuotaRepository,
            allocationRepository,
            roleRepository,
            schoolUserRepository,
            schoolQuotaPolicyRepository,
            schoolBalanceRepository);

        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);

        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));

        when(roleRepository.findByCode(any()))
            .thenReturn(Optional.of(Role.builder()
            .id(roleId)
            .build()));

        when(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(eq(schoolId), any()))
            .thenReturn(SchoolQuotaPolicy.fullyDistributable(schoolId, QuotaType.ASSESSMENT));

        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());

        givenPool(10_000_000, 0);
        givenEligible(teacherA, teacherB);
        givenExistingAllocations();
        givenAllocationSums(0, 0);
    }

    // ---------------------------------------------------------------------
    // People who are no longer eligible
    // ---------------------------------------------------------------------

    @Test
    void manual_allocation_ignores_rows_of_users_who_are_no_longer_eligible() {
        // The person who left holds 6 million on paper. Adding it makes 8 + 6 > 10 and this split is rejected
        // -- while the table only shows two teachers totalling 0 VND, so that rejection cannot be
        // matched against anything the user can see.
        givenExistingAllocations(allocation(formerTeacher, 6_000_000, 0));

        assertThatCode(() -> allocateManually(teacherA, 8_000_000)).doesNotThrowAnyException();

        verify(allocationRepository).upsertAllocation(
            subscriptionId, QuotaType.ASSESSMENT, teacherA, BigDecimal.valueOf(8_000_000));
    }

    @Test
    void manual_allocation_still_counts_rows_of_users_who_are_still_eligible() {
        // The other side of the same rule: skipping the person who left does NOT mean skipping everyone absent
        // from the request. A colleague still employed still has their share taking up room.
        givenExistingAllocations(allocation(teacherB, 6_000_000, 0));

        assertThatThrownBy(() -> allocateManually(teacherA, 8_000_000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds the distributable share");
    }

    @Test
    void view_page_reports_orphaned_allocation_separately_from_distributed() {
        // 6.5 million is on the ledger, but only 4 million belongs to users who are still eligible.
        // The difference must be named: if it is silently dropped, "already distributed" on screen
        // does not match the DB, and if it is added in, it eats the ceiling of the people who
        // actually need it.
        givenAllocationSums(4_000_000, 6_500_000);

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        assertThat(page.distributedAmountVnd()).isEqualByComparingTo("4000000");
        assertThat(page.orphanedAmountVnd()).isEqualByComparingTo("2500000");
    }

    // ---------------------------------------------------------------------
    // Automatic even split
    // ---------------------------------------------------------------------

    @Test
    void auto_split_distributes_exactly_the_distributable_amount() {
        UUID teacherC = UUID.randomUUID();
        givenEligible(teacherA, teacherB, teacherC);
        // 10,000,000 / 3 does not divide evenly -- the remainder must still be spread out and must
        // not evaporate.
        List<BigDecimal> amounts = captureAutoSplitAmounts(3);

        assertThat(amounts.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b)))
            .isEqualByComparingTo("10000000");
    }

    @Test
    void auto_split_does_not_overshoot_when_former_staff_still_hold_allocations() {
        // This is the old trap: the even split handed out the whole distributable amount to the eligible people, while
        // the row of the person who left was untouched -- so right after an "automatic even split"
        // the total exceeded the cap and every following manual edit was rejected. Now the total counts only eligible
        // people, so after the even split it fits the cap exactly, and a manual edit right after it still works.
        givenExistingAllocations(allocation(formerTeacher, 6_000_000, 0));
        captureAutoSplitAmounts(2);

        givenExistingAllocations(
            allocation(formerTeacher, 6_000_000, 0),
            allocation(teacherA, 5_000_000, 0),
            allocation(teacherB, 5_000_000, 0));

        assertThatCode(() -> allocateManually(teacherA, 5_000_000)).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------------
    // "Not distributed" differs from "distributed 0 dong"
    // ---------------------------------------------------------------------

    @Test
    void view_page_distinguishes_never_allocated_from_explicit_zero() {
        // If both show "0", the school admin cannot tell that half of their teachers have NO
        // spending ceiling at all -- see the javadoc of QuotaUserAllocationPageResponse.Row.
        givenExistingAllocations(allocation(teacherB, 0, 0));

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        QuotaUserAllocationPageResponse.Row rowA = page.content().stream().filter(row -> row.userId().equals(teacherA)).findFirst().orElseThrow();
        QuotaUserAllocationPageResponse.Row rowB = page.content().stream().filter(row -> row.userId().equals(teacherB)).findFirst().orElseThrow();

        assertThat(rowA.allocatedAmountVnd()).isNull();
        assertThat(rowA.usedAmountVnd()).isEqualByComparingTo("0");
        assertThat(rowB.allocatedAmountVnd()).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------------
    // Distribution cap vs ability to pay
    // ---------------------------------------------------------------------

    @Test
    void distributable_cap_ignores_pool_consumption_while_spendable_funds_do_not() {
        // The wallet has spent 7 of 10 million. The ceiling is still 10 million -- ON PURPOSE: a
        // personal spending ceiling must stay stable for the whole period; if it shrank every time
        // someone spent, the school admin would have to redistribute constantly. But the amount
        // that can really still be paid is only 3 million, and that is the number the screen must
        // warn about.
        givenPool(10_000_000, 7_000_000);

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        assertThat(page.distributableAmountVnd()).isEqualByComparingTo("10000000");
        assertThat(page.spendableFundsVnd()).isEqualByComparingTo("3000000");
    }

    @Test
    void spendable_funds_add_wallet_balance_but_never_subtract_debt() {
        // A negative balance is DEBT, and debt is already blocked at a separate gate with its own
        // message. Subtracting it here would turn a locked school into a school that has "run out
        // of quota" -- reporting the wrong reason, which only points to the wrong fix.
        givenPool(10_000_000, 7_000_000);
        givenWallet(-5_000_000);

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        assertThat(page.spendableFundsVnd()).isEqualByComparingTo("3000000");
        assertThat(page.walletBalanceVnd()).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------------
    // Balance-draw confirmation gate
    // ---------------------------------------------------------------------

    @Test
    void allocation_beyond_cap_needs_explicit_wallet_confirmation() {
        givenWallet(4_000_000);

        assertThatThrownBy(() -> allocateManually(teacherA, 12_000_000))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("This allocation exceeds the share from the plan and will draw an extra 2000000.000000 VND from the school's self-funded balance "
                + "(4000000 VND remaining). Confirm to continue.");

        assertThatCode(() -> service.distribute(
                schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.MANUAL,
                List.of(new AllocateUserQuotaAmountCommand(teacherA, BigDecimal.valueOf(12_000_000))), true))
            .doesNotThrowAnyException();
    }

    @Test
    void allocation_beyond_cap_and_wallet_is_refused_even_with_confirmation() {
        // The confirmation is so the user knows they are eating into the shared balance, not a way around the limit.
        givenWallet(1_000_000);

        assertThatThrownBy(() -> service.distribute(
                schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.MANUAL,
                List.of(new AllocateUserQuotaAmountCommand(teacherA, BigDecimal.valueOf(12_000_000))), true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("self-funded balance");
    }

    @Test
    void allocation_below_already_spent_amount_is_refused() {
        givenExistingAllocations(allocation(teacherA, 5_000_000, 3_000_000));

        assertThatThrownBy(() -> allocateManually(teacherA, 1_000_000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lower than the amount already used");
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private void allocateManually(UUID userId, long amountVnd) {
        service.distribute(
            schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.MANUAL,
            List.of(new AllocateUserQuotaAmountCommand(userId, BigDecimal.valueOf(amountVnd))), false);
    }

    /**
     * Runs the even distribution and returns the amounts that were written -- used to add them back
     * up and check against the ceiling.
     */
    private List<BigDecimal> captureAutoSplitAmounts(int expectedUsers) {
        service.distribute(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.AUTO, null, false);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(allocationRepository, times(expectedUsers)).upsertAllocation(
            eq(subscriptionId), eq(QuotaType.ASSESSMENT), any(), amountCaptor.capture());
        return amountCaptor.getAllValues();
    }

    private void givenPool(long totalVnd, long usedVnd) {
        SchoolSubscriptionQuotaRecord pool = SchoolSubscriptionQuotaRecord.builder()
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.ASSESSMENT)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(totalVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .build();
        pool.setId(UUID.randomUUID());
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(Optional.of(pool));
    }

    private void givenEligible(UUID... userIds) {
        List<SchoolUser> content = java.util.Arrays.stream(userIds)
            .map(userId -> SchoolUser.builder()
                .schoolId(schoolId)
                .userId(userId)
                .startDate(null)
                .endDate(null)
                .build())
            .toList();
        when(schoolUserRepository.findBySchoolId(
                any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(content, 1, 20, content.size(), 1));
    }

    private void givenExistingAllocations(SchoolSubscriptionQuotaUserAllocation... allocations) {
        when(allocationRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(List.of(allocations));
    }

    private void givenAllocationSums(long eligibleVnd, long allVnd) {
        when(allocationRepository.sumAllocatedForEligibleUsers(any(), any(), any(), any(), any()))
            .thenReturn(BigDecimal.valueOf(eligibleVnd));
        when(allocationRepository.sumAllocated(any(), any())).thenReturn(BigDecimal.valueOf(allVnd));
    }

    private void givenWallet(long balanceVnd) {
        when(schoolBalanceRepository.findBySchoolId(schoolId))
            .thenReturn(Optional.of(SchoolBalance.builder()
                .schoolId(schoolId)
                .balanceVnd(BigDecimal.valueOf(balanceVnd))
                .createdAt(null)
                .updatedAt(null)
                .build()));
    }

    private SchoolSubscriptionQuotaUserAllocation allocation(UUID userId, long allocatedVnd, long usedVnd) {
        return SchoolSubscriptionQuotaUserAllocation.builder()
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.ASSESSMENT)
            .userId(userId)
            .allocatedAmountVnd(BigDecimal.valueOf(allocatedVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .build();
    }
}
