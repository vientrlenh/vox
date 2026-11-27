package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;

/**
 * The append-only ledger of SchoolBalance -- DELIBERATELY has no update/delete: editing a recorded entry is wrong by
 * business rule, to adjust you append an ADJUSTMENT row.
 *
 * <p>Every time range here is HALF-OPEN {@code [from, to)} -- see the javadoc of each method.
 */
public interface SchoolBalanceEntryRepository {
    Optional<SchoolBalanceEntry> findById(UUID id);
    SchoolBalanceEntry save(SchoolBalanceEntry entry);

    /**
     * Statement of the school, newest first (matches idx_school_balance_entries_school_occurred).
     * {@code page} counts FROM 1 per the project-wide convention.
     *
     * @param entryType null = no filter by entry type
     * @param from inclusive, {@code to} is NOT inclusive. Both MUST be non-null -- a caller who wants
     *     "the whole history" passes {@link Instant#EPOCH} and the current time. Deliberately
     *     does not accept null: a null Instant parameter in JPQL has to be wrapped in a CAST so Postgres can infer
     *     the type, and that trap is not worth trading for a convenience parameter.
     */
    PageResult<SchoolBalanceEntry> findBySchoolId(
        UUID schoolId, SchoolBalanceEntryType entryType, Instant from, Instant to, int page, int size);

    /** @param from inclusive, {@code to} is NOT inclusive -- two consecutive periods do not count the shared boundary twice. */
    List<SchoolBalanceEntry> findBySchoolIdInRange(UUID schoolId, Instant from, Instant to);

    /**
     * Idempotency guard for the money-adding path, matching exactly the column pair of
     * uq_school_balance_entries_order (order_id, entry_type): the gateway webhook and PendingOrderReconciler
     * may settle the same order, and without this guard the money enters the balance twice.
     */
    boolean existsByOrderIdAndEntryType(UUID orderId, SchoolBalanceEntryType entryType);

    /** Returns 0 (not null) when there are no entries. {@code from} is inclusive, {@code to} is not. */
    BigDecimal sumAmountBySchoolIdAndEntryTypeInRange(
        UUID schoolId, SchoolBalanceEntryType entryType, Instant from, Instant to);
}
