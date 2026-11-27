package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.CarryQuotaFundingAtPeriodStartService;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

/**
 * Pins the self-funded money carried over at EXACTLY the boundary between two periods -- the other half of the early-renewal bug.
 *
 * <p>{@code OrderSettlementQuotaCarryForwardTests} guards the other half: at order settlement the figure must NOT be fixed. This
 * file guards what happens when the new period really starts, and the three properties here are why a late carry-over is safer
 * than an early snapshot:
 *
 * <ul>
 *   <li><b>Read late</b> so it sees the right figure after the old period has finished spending/topping up.</li>
 *   <li><b>Add, don't assign</b> so it does not erase what the school topped up into the new period while waiting for the job.</li>
 *   <li><b>Exactly once</b> because the marker is cleared right away -- adding twice doubles the money.</li>
 * </ul>
 */
class CarryQuotaFundingAtPeriodStartServiceTests {

    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private CarryQuotaFundingAtPeriodStartService service;

    private final UUID previousSubscriptionId = UUID.randomUUID();
    private final UUID newSubscriptionId = UUID.randomUUID();
    private final UUID newPracticePoolId = UUID.randomUUID();
    private final Instant now = Instant.parse("2027-01-01T00:00:00Z");

    private final List<SchoolSubscriptionQuotaRecord> sourcePools = new ArrayList<>();

    @BeforeEach
    void setUp() {
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        service = new CarryQuotaFundingAtPeriodStartService(quotaRecordRepository);

        when(quotaRecordRepository.findBySchoolSubscriptionId(previousSubscriptionId))
            .thenReturn(sourcePools);
    }

    @Test
    void nothing_to_do_when_no_period_has_a_pending_carry() {
        when(quotaRecordRepository.findDueFundingCarries(now)).thenReturn(List.of());

        assertThat(service.carryDueFunding(now)).isZero();
        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
    }

    @Test
    void the_unspent_funding_of_the_closing_period_lands_in_the_new_one() {
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        assertThat(service.carryDueFunding(now)).isEqualTo(1);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(5_000_000)));
    }

    @Test
    void the_amount_is_read_at_the_boundary_so_spending_in_the_window_reduces_it() {
        // The whole point of deferring: the school spends 12M on the old period AFTER paying for the renewal. A snapshot at
        // settlement time would say 5M; the correct figure at the boundary is 3M.
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 12_000_000, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(3_000_000)));
    }

    @Test
    void funding_added_during_the_window_is_carried_too_instead_of_being_destroyed() {
        // The opposite direction: the school TOPS UP 3M after paying for the renewal. That money goes into the old period's quota record, is not
        // in the snapshot taken at settlement, and before V13 it died at the boundary.
        givenSourcePool(QuotaType.PRACTICE, 18_000_000, 0, 8_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(8_000_000)));
    }

    @Test
    void carrying_adds_to_the_new_pool_rather_than_overwriting_what_it_already_holds() {
        // The new period has already started and the school topped up 2M into it before the job could run. An ASSIGNMENT
        // "funded = unspent part of the old period" would erase exactly that amount.
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE, 2_000_000);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(5_000_000)));
        verify(quotaRecordRepository, never()).save(any());
    }

    @Test
    void the_promise_is_cleared_so_a_second_run_carries_nothing_twice() {
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).clearFundingCarry(newPracticePoolId);
    }

    @Test
    void the_promise_is_cleared_even_when_there_is_nothing_left_to_carry() {
        // The old period is spent out. Without clearing the marker the job re-reads this row every hour, forever.
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 15_000_000, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        assertThat(service.carryDueFunding(now)).isZero();

        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
        verify(quotaRecordRepository).clearFundingCarry(newPracticePoolId);
    }

    @Test
    void a_quota_type_the_new_plan_dropped_is_left_alone_rather_than_carried_somewhere_else() {
        // The old period still has money in the ASSESSMENT wallet, the new period only has PRACTICE. There is no commitment for ASSESSMENT, so that amount
        // stays behind -- the service must NOT wrongly push it into the other wallet.
        givenSourcePool(QuotaType.ASSESSMENT, 10_000_000, 0, 4_000_000);
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(5_000_000)));
        verify(quotaRecordRepository, never()).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(4_000_000)));
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private void givenSourcePool(QuotaType quotaType, long totalVnd, long usedVnd, long fundedVnd) {
        sourcePools.add(SchoolSubscriptionQuotaRecord.builder()
            .id(UUID.randomUUID())
            .schoolSubscriptionId(previousSubscriptionId)
            .quotaType(quotaType)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(totalVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .fundedFromBalanceVnd(BigDecimal.valueOf(fundedVnd))
            .build());
    }

    private void givenDueCarry(QuotaType quotaType) {
        givenDueCarry(quotaType, 0);
    }

    /**
     * A wallet for the new period that has reached its run date and is still scheduled.
     * {@code alreadyFundedVnd} = what the school topped up on its own while waiting.
     */
    private void givenDueCarry(QuotaType quotaType, long alreadyFundedVnd) {
        SchoolSubscriptionQuotaRecord target = SchoolSubscriptionQuotaRecord.builder()
            .id(newPracticePoolId)
            .schoolSubscriptionId(newSubscriptionId)
            .quotaType(quotaType)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(10_000_000 + alreadyFundedVnd))
            .usedAmountVnd(BigDecimal.ZERO)
            .fundedFromBalanceVnd(BigDecimal.valueOf(alreadyFundedVnd))
            .build();
        target.setCarryFundingFromSubscriptionId(previousSubscriptionId);
        when(quotaRecordRepository.findDueFundingCarries(now)).thenReturn(List.of(target));
    }
}
