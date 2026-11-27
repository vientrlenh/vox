package com.sep.vox.application.usecase.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.balance.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceSummaryUseCase;
import com.sep.vox.application.response.input.balance.SchoolBalanceSummaryResponse;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;

/**
 * The totals box of the statement page exists to explain EVERY change of the balance within the range. That is
 * only true when it sums every type of balance entry -- and once it was not true: QUOTA_FUNDING was added to the
 * enum in V12 without being added here, so a school that moved 5M into the quota pool saw a statement showing a
 * balance drop of 5M with all three totals at zero.
 *
 * <p>So the main check here walks {@code SchoolBalanceEntryType.values()} instead of listing them by hand:
 * listing by hand repeats exactly the list that was forgotten.
 */
class ViewSchoolBalanceSummaryUseCaseTests {

    private SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private SchoolScopedReadGuard schoolScopedReadGuard;
    private ViewSchoolBalanceSummaryUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final Instant from = Instant.parse("2026-08-01T00:00:00Z");
    private final Instant to = Instant.parse("2026-09-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);
        schoolScopedReadGuard = mock(SchoolScopedReadGuard.class);
        useCase = new ViewSchoolBalanceSummaryUseCase(schoolBalanceEntryRepository, schoolScopedReadGuard);

        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(any(), any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void every_entry_type_is_summed_so_the_tiles_account_for_the_whole_balance_change() {
        useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        for (SchoolBalanceEntryType entryType : SchoolBalanceEntryType.values()) {
            verify(schoolBalanceEntryRepository)
                .sumAmountBySchoolIdAndEntryTypeInRange(eq(schoolId), eq(entryType), eq(from), eq(to));
        }
    }

    @Test
    void quota_funding_is_reported_on_its_own_rather_than_folded_into_the_overage_bucket() {
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.OVERAGE_CHARGE), any(), any()))
            .thenReturn(new BigDecimal("-120000"));
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.QUOTA_FUNDING), any(), any()))
            .thenReturn(new BigDecimal("-5000000"));

        SchoolBalanceSummaryResponse summary = useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        // Merging these two numbers would tell the principal that the school spent 5.12m on AI,
        // when in fact it spent only 120k -- the other 5m is still the school's money, merely
        // sitting in the quota wallet.
        assertThat(summary.overageChargedVnd()).isEqualTo("-120000");
        assertThat(summary.quotaFundedVnd()).isEqualTo("-5000000");
    }

    @Test
    void top_up_and_refund_stay_folded_together_because_both_read_as_money_in() {
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.TOP_UP), any(), any()))
            .thenReturn(new BigDecimal("2000000"));
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.REFUND), any(), any()))
            .thenReturn(new BigDecimal("300000"));

        SchoolBalanceSummaryResponse summary = useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        assertThat(summary.creditedVnd()).isEqualTo("2300000");
    }

    @Test
    void a_school_with_no_entries_reads_zero_everywhere_instead_of_failing() {
        SchoolBalanceSummaryResponse summary = useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        assertThat(summary.creditedVnd()).isEqualTo("0");
        assertThat(summary.overageChargedVnd()).isEqualTo("0");
        assertThat(summary.quotaFundedVnd()).isEqualTo("0");
        assertThat(summary.adjustedVnd()).isEqualTo("0");
    }
}
