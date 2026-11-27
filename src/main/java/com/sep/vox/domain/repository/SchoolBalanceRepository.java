package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolBalance;

/**
 * Each school has EXACTLY ONE balance row (uq_school_balances_school), so every credit/debit contends
 * on the same row.
 *
 * <p>DELIBERATELY has no unlocked "UPDATE ... WHERE" style {@code tryDebit}/{@code credit}: every balance change
 * must come with a SchoolBalanceEntry, and the entry has the column {@code balance_after_vnd NOT NULL}.
 * An in-place UPDATE that adds or subtracts does not return the balance AFTER the subtraction, while a SELECT afterwards may
 * already see the result of another transaction slipping in -- two entries would record the same balanceAfter and
 * break the invariant SUM(entries.amountVnd) = balanceVnd.
 *
 * <p>So the ONLY write path is: {@link #findBySchoolIdForUpdateOrCreate} (lock the row) -> compute the new
 * balance in Java -> {@link #save} + write the entry, all in the same transaction. The lock is held only on one
 * row of one school for a few statements, which is not a bottleneck at this scale.
 */
public interface SchoolBalanceRepository {
    Optional<SchoolBalance> findById(UUID id);

    /**
     * Read only (e.g. a guard checking there is enough money). To WRITE you must go through
     * {@link #findBySchoolIdForUpdateOrCreate}.
     */
    Optional<SchoolBalance> findBySchoolId(UUID schoolId);

    /**
     * Locks the balance row (SELECT ... FOR UPDATE) until the end of the transaction, building the row if the school has never
     * had one. Mandatory before any balance change -- see the javadoc of the interface.
     *
     * <p>CREATING is folded in here instead of letting callers do {@code orElseGet(emptyFor(...))} themselves: a row
     * lock cannot lock a row that does not exist yet, so each caller building it itself is a separate
     * race window, and both write paths (top-up and overage charge) once opened exactly that window.
     *
     * <p>Returns SchoolBalance directly rather than Optional: after this call the row certainly exists, so
     * an Optional would only invite callers to rewrite the very "create if missing" branch that was just removed.
     */
    SchoolBalance findBySchoolIdForUpdateOrCreate(UUID schoolId, Instant now);

    SchoolBalance save(SchoolBalance balance);
}
