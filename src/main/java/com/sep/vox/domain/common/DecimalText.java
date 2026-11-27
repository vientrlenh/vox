package com.sep.vox.domain.common;

import java.math.BigDecimal;

/**
 * Puts a money amount on the wire as a lossless decimal string.
 *
 * <p>Used for every {@code numeric(18,6)} column of the school balance (school_balances, school_balance_entries,
 * school_debt_events). Those columns deliberately keep 6 decimal places because one practice run can cost only a
 * few hundredths of a dong, so they do NOT satisfy the premise of the GraphQL schema Float convention ("VND is always
 * whole"). {@code 128440.95} cannot be represented exactly as a double, and this is precisely where the number must
 * match exactly: the ledger invariant is {@code SUM(amount_vnd) = balance_vnd}.
 *
 * <p>{@code toPlainString()} rather than {@code toString()}: {@code toString()} switches to scientific notation
 * when the exponent is small enough ("1E-7"), and a client parsing that string gets a completely different amount.
 * The string here keeps the stored scale, so "-128440.950000" rather than "-128440.95" -- on purpose, so that two
 * reads of the same entry always produce the same string, whether or not the value is a whole dong.
 *
 * <p>Converted in the Java layer and not with {@code str()} in JPQL: this string is now part of the CONTRACT with
 * clients, so it must be built deterministically by our own code, not by however some Postgres version renders
 * {@code cast(numeric as varchar)}.
 */
public final class DecimalText {

    private DecimalText() {}

    /** null in, null out -- nullable money columns (triggerAmountVnd, costUsd) keep the meaning "not present". */
    public static String of(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /**
     * For a {@code NOT NULL} column where an aggregate query may return null (SUM over 0 rows). A school with no
     * entries yet is the MOST COMMON case, not an edge case, so it must come out as "0" instead of breaking a
     * non-null field on the schema.
     */
    public static String orZero(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
