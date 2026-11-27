package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.AiUsageRecord;

public interface AiUsageRecordRepository {
    Optional<AiUsageRecord> findById(UUID id);
    AiUsageRecord save(AiUsageRecord record);
    List<AiUsageRecord> findByAssessmentSessionId(UUID assessmentSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    /**
     * CLAIMS every UNCHARGED cost row of the session by stamping them all with one shared {@code chargedAt}, returning
     * the number of rows claimed. That stamp is the identity tag of this charge -- read back with
     * {@link #sumCostVndByAssessmentSessionIdAndChargedAt}.
     *
     * <p>It exists because a session is allowed to be regraded (UpdateAssessmentSessionStatusUseCase permits GRADED ->
     * GRADING): the later grading produces new real cost that must be charged, but if the whole session were summed again the
     * share of the earlier grading would be charged a second time. The unit of "may only be charged once" is the INDIVIDUAL cost
     * ROW, not the assessment session.
     *
     * <p>Claim first, sum afterwards: in the opposite direction, a usage row inserted by Kafka between the two
     * steps would be stamped as charged without ever being added to the deduction -- that amount would be lost entirely.
     */
    int markChargedByAssessmentSessionId(UUID assessmentSessionId, Instant chargedAt);

    /**
     * WAIVES every unsettled cost row of the session -- they belong to the grading attempt that just failed.
     *
     * <p>The rule is "charge for the work that PRODUCES A USABLE RESULT", not "forgive what fails": a
     * failed grading attempt leaves no {@code assessment_candidate_results} row so the school received
     * nothing. Do not extend this rule to the speaking practice path, where the money is charged right in the request according to
     * the cost already incurred because that speaking turn HAS delivered a result to the student -- see
     * {@code SubmitPracticeTurnUseCase}.
     *
     * @return the number of rows just waived
     */
    int markWaivedByAssessmentSessionId(UUID assessmentSessionId, Instant waivedAt);

    /**
     * The sum of cost_vnd of the rows just claimed by {@link #markChargedByAssessmentSessionId} at this stamp --
     * the real source for deducting from SchoolSubscriptionQuotaRecord.
     *
     * <p>It sums the cost_vnd already fixed on each row and does NOT convert the USD total at today's rate: each row
     * recorded the exchange rate at the moment the cost was incurred (fx_rate_used), so an assessment session spanning a day when the rate changes
     * still yields the correct real money, and summing again does not produce a different number.
     */
    BigDecimal sumCostVndByAssessmentSessionIdAndChargedAt(UUID assessmentSessionId, Instant chargedAt);

    /**
     * Same as above but keeps the original currency, to be written to school_balance_entries.cost_usd for reconciliation
     * against the provider invoice. It must look at the SAME chargedAt boundary as the VND version, otherwise the two columns of
     * the same entry would describe two different sets of rows.
     */
    BigDecimal sumCostUsdByAssessmentSessionIdAndChargedAt(UUID assessmentSessionId, Instant chargedAt);
}
