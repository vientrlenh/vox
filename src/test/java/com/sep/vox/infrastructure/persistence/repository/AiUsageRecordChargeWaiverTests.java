package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.sep.vox.support.RepositoryTest;

/**
 * Runs REAL SQL: the whole value of V9 is that the two UPDATE statements do not step on each other, and that can only be
 * checked on a real database with real constraints.
 *
 * <p>The rule being protected: a school is charged for AI work that PRODUCED A USABLE RESULT. A failed grading attempt
 * produces nothing so there is nothing to charge -- but an earlier SUCCESSFUL grading attempt does, and
 * must not be refunded.
 */
@RepositoryTest
class AiUsageRecordChargeWaiverTests {

    @Autowired
    private SpringDataAiUsageRecordRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * THE TRAP V9 WAS CREATED TO BLOCK: a failed grading attempt burns tokens, the successful
     * regrade calls markCharged() and previously swallowed the failed attempt's share too -- the
     * school paid for both runs.
     */
    @Test
    void shouldNotChargeTheFailedRoundWhenALaterRetrySucceeds() {
        UUID sessionId = UUID.randomUUID();
        insertUsage(sessionId, "40000");  // failed attempt
        repository.markWaivedByAssessmentSessionId(sessionId, Instant.now());

        insertUsage(sessionId, "100000"); // regrade attempt
        Instant chargedAt = Instant.now();
        int claimed = repository.markChargedByAssessmentSessionId(sessionId, chargedAt);

        assertThat(claimed).isEqualTo(1);
        assertThat(repository.sumCostVndByAssessmentSessionIdAndChargedAt(sessionId, chargedAt))
            .isEqualByComparingTo("100000");
        assertThat(repository.sumWaivedCostVndByAssessmentSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /**
     * The opposite direction: the waiver must NOT touch a row that has already been charged. A
     * session graded correctly and only failing on a later attempt -- letting the waiver overwrite
     * the first attempt would refund work that was fully delivered.
     */
    @Test
    void shouldNotWaiveCostThatWasAlreadyCharged() {
        UUID sessionId = UUID.randomUUID();
        insertUsage(sessionId, "100000");
        Instant chargedAt = Instant.now();
        repository.markChargedByAssessmentSessionId(sessionId, chargedAt);

        insertUsage(sessionId, "40000");
        int waived = repository.markWaivedByAssessmentSessionId(sessionId, Instant.now());

        assertThat(waived).isEqualTo(1);
        assertThat(repository.sumCostVndByAssessmentSessionIdAndChargedAt(sessionId, chargedAt))
            .isEqualByComparingTo("100000");
        assertThat(repository.sumWaivedCostVndByAssessmentSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /**
     * Waiving twice does not double up: the second time there is no unsettled row left to touch.
     */
    @Test
    void shouldBeIdempotentAcrossRepeatedWaives() {
        UUID sessionId = UUID.randomUUID();
        insertUsage(sessionId, "40000");

        assertThat(repository.markWaivedByAssessmentSessionId(sessionId, Instant.now())).isEqualTo(1);
        assertThat(repository.markWaivedByAssessmentSessionId(sessionId, Instant.now())).isZero();
        assertThat(repository.sumWaivedCostVndByAssessmentSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /**
     * A failed session that NOBODY regrades must still be waived. Before V9 the behavior depended
     * on whether someone went to fix it -- left alone it was free, fixed it and the failed attempt
     * was charged too.
     */
    @Test
    void shouldWaiveEvenWhenNobodyEverRetries() {
        UUID sessionId = UUID.randomUUID();
        insertUsage(sessionId, "40000");
        repository.markWaivedByAssessmentSessionId(sessionId, Instant.now());

        assertThat(repository.markChargedByAssessmentSessionId(sessionId, Instant.now())).isZero();
    }

    private void insertUsage(UUID assessmentSessionId, String costVnd) {
        em.createNativeQuery("""
            INSERT INTO ai_usage_records (id, assessment_session_id, turn_id, usage_event_id, usage_type, provider,
                                          unit_price_json, cost_usd, cost_vnd, fx_rate_used, occurred_at)
            VALUES (:id, :sessionId, :turnId, :eventId, 'LLM_TOKEN', 'openai',
                    '{}', 1.0, :costVnd, 25000, :now)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("sessionId", assessmentSessionId)
            .setParameter("turnId", UUID.randomUUID())
            .setParameter("eventId", UUID.randomUUID())
            .setParameter("costVnd", new BigDecimal(costVnd))
            .setParameter("now", Instant.now())
            .executeUpdate();
        em.flush();
        em.clear();
    }
}
