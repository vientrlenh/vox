package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.command.subscription.FundQuotaFromBalanceCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.subscription.FundQuotaFromBalanceUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.QuotaFundingResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Funding from the self-funded balance into the quota pool -- a transfer between two pockets of the same school.
 *
 * <p>This suite watches three things, and all three are reasons the "deduct the balance when dividing among
 * individuals" option was dropped:
 *
 * <ul>
 *   <li><b>The money leaves the balance exactly ONCE.</b> The balance goes down, the quota pool goes up by exactly
 *       that amount, and there is no second deduction -- because {@code tryConsume} afterwards sees room in the
 *       quota pool, so {@code ConsumeQuotaService.chargeOverage} never runs.</li>
 *   <li><b>The row is locked before deducting.</b> The ONLY balance write path goes through
 *       {@code findBySchoolIdForUpdateOrCreate}; reading with {@code findBySchoolId} and then subtracting would
 *       let two administrators who click at the same time see the same balance and both spend it.</li>
 *   <li><b>balanceAfter of the entry comes from that very deduction</b>, not recomputed -- otherwise the statement
 *       and the aggregate balance drift apart and the invariant SUM(entries) = balance_vnd breaks.</li>
 * </ul>
 */
class FundQuotaFromBalanceUseCaseTests {

    private UserContextPort userContextPort;
    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private FundQuotaFromBalanceUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID quotaId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        SchoolSubscriptionRepository schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);

        useCase = new FundQuotaFromBalanceUseCase(
            userContextPort,
            schoolSubscriptionRepository,
            quotaRecordRepository,
            schoolBalanceRepository,
            schoolBalanceEntryRepository,
            new SchoolSubscriptionDebtGuardService(schoolBalanceRepository));

        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(actorId);

        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));

        givenPool(10_000_000, 10_000_000, 0);
        givenWallet(2_000_000);
    }

    @Test
    void funding_moves_money_from_the_wallet_into_the_quota_pool() {
        QuotaFundingResponse response = useCase.execute(fund(500_000));

        verify(quotaRecordRepository).addFundingFromBalance(eq(quotaId), eq(BigDecimal.valueOf(500_000)));
        assertThat(response.fundedAmountVnd()).isEqualByComparingTo("500000");
        assertThat(response.balanceAfterVnd()).isEqualByComparingTo("1500000");
    }

    @Test
    void funding_debits_the_wallet_exactly_once_and_records_one_entry() {
        useCase.execute(fund(500_000));

        SchoolBalanceEntry entry = captureEntry();
        assertThat(entry.getEntryType()).isEqualTo(SchoolBalanceEntryType.QUOTA_FUNDING);
        // Negative: the money LEAVES the wallet. chk_school_balance_entries_quota_funding_traceable
        // requires amount_vnd < 0, and the factory flips the sign itself so the caller does not
        // have to remember.
        assertThat(entry.getAmountVnd()).isEqualByComparingTo("-500000");
        verify(schoolBalanceRepository).save(any());
        verify(schoolBalanceEntryRepository).save(any());
    }

    @Test
    void funding_takes_balance_after_from_the_row_it_just_debited() {
        useCase.execute(fund(500_000));

        SchoolBalanceEntry entry = captureEntry();
        ArgumentCaptor<SchoolBalance> balanceCaptor = ArgumentCaptor.forClass(SchoolBalance.class);
        verify(schoolBalanceRepository).save(balanceCaptor.capture());

        assertThat(entry.getBalanceAfterVnd())
            .isEqualByComparingTo(balanceCaptor.getValue().getBalanceVnd());
    }

    @Test
    void funding_locks_the_balance_row_before_debiting_it() {
        useCase.execute(fund(500_000));

        verify(schoolBalanceRepository).findBySchoolIdForUpdateOrCreate(eq(schoolId), any());
    }

    @Test
    void funding_carries_the_actor_and_quota_type_onto_the_entry() {
        // Both are REQUIRED at the DB level: this is a human decision, one-way and not refundable,
        // so the ledger must be able to say who clicked and which pocket the money went into.
        useCase.execute(fund(500_000));

        SchoolBalanceEntry entry = captureEntry();
        assertThat(entry.getActorId()).isEqualTo(actorId);
        assertThat(entry.getQuotaType()).isEqualTo(QuotaType.PRACTICE);
        assertThat(entry.getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Test
    void funding_entry_carries_no_ai_cost_columns() {
        // This is money moving between pockets, NOT an AI expense -- there is no vendor invoice to
        // reconcile against and no session that caused it. The DB constraint demands exactly that.
        useCase.execute(fund(500_000));

        SchoolBalanceEntry entry = captureEntry();
        assertThat(entry.getCostUsd()).isNull();
        assertThat(entry.getFxRateUsed()).isNull();
        assertThat(entry.getAssessmentSessionId()).isNull();
        assertThat(entry.getPracticeSessionId()).isNull();
        assertThat(entry.getOrderId()).isNull();
    }

    @Test
    void funding_is_refused_when_the_wallet_holds_less_than_requested() {
        assertThatThrownBy(() -> useCase.execute(fund(2_500_000)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient");

        verify(schoolBalanceEntryRepository, never()).save(any());
        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
    }

    @Test
    void funding_is_refused_with_the_debt_message_when_the_school_is_locked() {
        // A negative balance would also fail the "is there enough money" comparison below, but with the message
        // "insufficient balance" -- right symptom, wrong cause, and so the wrong remedy as well.
        givenWallet(-1_000_000);

        assertThatThrownBy(() -> useCase.execute(fund(500_000)))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("locked");

        verify(schoolBalanceRepository, never()).save(any());
        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
    }

    @Test
    void funding_is_refused_for_a_non_positive_amount() {
        assertThatThrownBy(() -> useCase.execute(fund(0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than 0");

        assertThatThrownBy(() -> useCase.execute(fund(-500_000)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than 0");

        verify(schoolBalanceRepository, never()).save(any());
    }

    @Test
    void funding_is_refused_for_a_school_the_admin_does_not_belong_to() {
        assertThatThrownBy(() -> useCase.execute(new FundQuotaFromBalanceCommand(
                UUID.randomUUID(), QuotaType.PRACTICE, BigDecimal.valueOf(500_000), null)))
            .isInstanceOf(ForbiddenException.class);

        verify(schoolBalanceRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private FundQuotaFromBalanceCommand fund(long amountVnd) {
        return new FundQuotaFromBalanceCommand(
            schoolId, QuotaType.PRACTICE, BigDecimal.valueOf(amountVnd), null);
    }

    private SchoolBalanceEntry captureEntry() {
        ArgumentCaptor<SchoolBalanceEntry> captor = ArgumentCaptor.forClass(SchoolBalanceEntry.class);
        verify(schoolBalanceEntryRepository).save(captor.capture());
        return captor.getValue();
    }

    private void givenPool(long totalVnd, long usedVnd, long fundedVnd) {
        SchoolSubscriptionQuotaRecord pool = SchoolSubscriptionQuotaRecord.builder()
            .id(quotaId)
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.PRACTICE)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(totalVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .fundedFromBalanceVnd(BigDecimal.valueOf(fundedVnd))
            .build();
        when(quotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.PRACTICE))
            .thenReturn(Optional.of(pool));
        when(quotaRecordRepository.findById(quotaId)).thenReturn(Optional.of(pool));
    }

    /**
     * The SAME instance for both the read path (debt guard) and the write path (row lock): the use
     * case calls {@code apply()} on the record that holds the lock, and the test must see exactly
     * the number that was changed.
     */
    private void givenWallet(long balanceVnd) {
        SchoolBalance balance = SchoolBalance.builder()
            .schoolId(schoolId)
            .balanceVnd(BigDecimal.valueOf(balanceVnd))
            .createdAt(null)
            .updatedAt(null)
            .build();
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.of(balance));
        when(schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(eq(schoolId), any())).thenReturn(balance);
    }
}
