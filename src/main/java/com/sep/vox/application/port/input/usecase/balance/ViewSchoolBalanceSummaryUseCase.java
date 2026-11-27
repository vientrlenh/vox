package com.sep.vox.application.port.input.usecase.balance;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.balance.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.balance.SchoolBalanceSummaryResponse;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;

/**
 * Sums up the balance ledger over a time range.
 *
 * <p>Five separate sums rather than one GROUP BY: each one reuses the existing
 * {@code sumAmountBySchoolIdAndEntryTypeInRange} (already COALESCEd to 0, already half-open on the range), and
 * all five run on {@code idx_school_balance_entries_school_occurred}. Adding a new SQL statement just to merge
 * five sums into one scan would trade something that is correct for something that has to be re-verified.
 *
 * <p>It must sum EVERY type of {@code SchoolBalanceEntryType}: the totals box on the statement page exists to
 * explain every change of the balance, so missing one type leaves a drop that nobody can explain.
 * QUOTA_FUNDING was missed in exactly that way once.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolBalanceSummaryUseCase
        implements IUseCase<ViewSchoolBalanceSummaryQuery, SchoolBalanceSummaryResponse> {

    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolScopedReadGuard schoolScopedReadGuard;

    @Override
    @Transactional(readOnly = true)
    public SchoolBalanceSummaryResponse execute(ViewSchoolBalanceSummaryQuery input) {
        schoolScopedReadGuard.requireCanRead(input.schoolId());

        Instant from = input.from() == null ? Instant.EPOCH : input.from();
        Instant to = input.to() == null ? Instant.now() : input.to();

        return SchoolBalanceSummaryResponse.of(
            sum(input, SchoolBalanceEntryType.TOP_UP, from, to),
            sum(input, SchoolBalanceEntryType.REFUND, from, to),
            sum(input, SchoolBalanceEntryType.OVERAGE_CHARGE, from, to),
            sum(input, SchoolBalanceEntryType.QUOTA_FUNDING, from, to),
            sum(input, SchoolBalanceEntryType.ADJUSTMENT, from, to)
        );
    }

    private BigDecimal sum(
            ViewSchoolBalanceSummaryQuery input, SchoolBalanceEntryType entryType, Instant from, Instant to) {
        return schoolBalanceEntryRepository
            .sumAmountBySchoolIdAndEntryTypeInRange(input.schoolId(), entryType, from, to);
    }
}
