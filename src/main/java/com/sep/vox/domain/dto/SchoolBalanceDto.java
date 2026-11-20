package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolBalance;

/**
 * The school's self-top-up wallet, in read form.
 *
 * <p>{@code id} and {@code version} are not exposed: no screen needs to point at the balance row -- it
 * is a summary, exactly one row per school, with nothing to link to. {@code version} even less: it is
 * Hibernate's optimistic-locking mechanism, and exposing it would invite the client to invent a write
 * flow the backend does not have (the only write path goes through findBySchoolIdForUpdateOrCreate).
 */
public record SchoolBalanceDto(
    UUID schoolId,
    String balanceVnd,
    boolean locked,
    Instant updatedAt
) {

    public static SchoolBalanceDto toDto(SchoolBalance balance) {
        return new SchoolBalanceDto(
            balance.getSchoolId(),
            DecimalText.of(balance.getBalanceVnd()),
            // Ask the domain model directly instead of rewriting the comparison with 0: isInDebt() is exactly
            // the function SchoolSubscriptionDebtGuardService.isSchoolLocked calls to block opening an
            // assessment session. Retyping the rule here builds a second copy, and that copy would not
            // change if the other rule changes.
            balance.isInDebt(),
            balance.getUpdatedAt()
        );
    }

    /**
     * The school has never touched the wallet. The balance row is created lazily (only on the first
     * write), and an empty wallet and a 0-dong wallet mean the SAME thing -- see
     * {@link SchoolBalance#emptyFor}. Returning null to GraphQL would only make the client add a branch for
     * the same state, so a 0-dong copy is built here.
     *
     * <p>Do NOT use {@code SchoolBalance.emptyFor} and then map: that function builds an aggregate for
     * WRITING, and its javadoc forbids using it on the read path.
     */
    public static SchoolBalanceDto emptyFor(UUID schoolId) {
        return new SchoolBalanceDto(schoolId, DecimalText.of(BigDecimal.ZERO), false, null);
    }
}
