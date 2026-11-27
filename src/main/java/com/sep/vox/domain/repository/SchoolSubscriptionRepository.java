package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;

public interface SchoolSubscriptionRepository {
    Optional<SchoolSubscription> findById(UUID id);
    SchoolSubscription save(SchoolSubscription subscription);
    /**
     * The subscription period IN EFFECT right now -- filtered by date, not just by status. A school that renews
     * early will have a period that is paid but not yet at its run date; that period is NOT returned here.
     */
    Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId);

    /**
     * EVERY period not yet ended at {@code at}, sorted by endDate DESCENDING -- including the running period and the periods already
     * paid and queued waiting.
     *
     * <p>Returns the whole list and not only the furthest period because an upgrade must close ALL and compensate ALL
     * -- missing a queued period would close a period the school already paid for without refunding a dong.
     * A caller that only needs the continuation point takes the first element.
     */
    List<SchoolSubscription> findUnfinishedBySchoolId(UUID schoolId, Instant at);

    /**
     * The school's most recent period regardless of status -- including expired ones, to support late renewal.
     */
    Optional<SchoolSubscription> findMostRecentBySchoolId(UUID schoolId);
    List<SchoolSubscription> findBySchoolId(UUID schoolId);
    PageResult<SchoolSubscription> findForAdmin(UUID planId, SchoolSubscriptionStatus status, String keyword, int page, int size);
    boolean existsActiveByPlanId(UUID planId);

    /**
     * Bulk transition ACTIVE -> EXPIRED for subscriptions past {@code cutoff}. Returns the number of rows
     * changed.
     *
     * <p>{@code cutoff} is EXCLUSIVE: it expires only when {@code endDate < cutoff}. The caller decides
     * what that moment means -- see {@code SubscriptionExpiryJob}.
     */
    int expireOverdue(Instant cutoff);

    // Id of the active plan of the school that a user belongs to (used to deduct quota)
    Optional<UUID> findActiveSubscriptionIdForUser(UUID userId);

    /**
     * The amount (VND) still spendable for one practice run of a user: the smaller of "the school's PRACTICE quota
     * remaining + the self-funded balance" and "the user's own personal quota remaining" (if the school
     * distributed one to them). ZERO if there is no active plan.
     *
     * <p>Called FUNDS and not QUOTA because this figure includes the self-funded balance: since ConsumeQuotaService
     * charges the part over the quota to that balance, "is there quota left" is no longer a question that can decide
     * whether practice may continue.
     */
    BigDecimal findPracticeSpendableFundsVnd(UUID userId);

    // Maximum minutes per practice run of the active plan (null if there is no plan)
    Integer findMaxTimePerAttemptMinForUser(UUID userId);
}
