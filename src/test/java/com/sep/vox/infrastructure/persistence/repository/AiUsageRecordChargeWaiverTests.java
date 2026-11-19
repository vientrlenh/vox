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
 * Chạy SQL THẬT: toàn bộ giá trị của V9 nằm ở chỗ hai câu UPDATE không giẫm lên nhau, và điều đó chỉ
 * kiểm được trên một database thật với ràng buộc thật.
 *
 * <p>Quy tắc đang bảo vệ: trường bị thu tiền cho phần việc AI đã TẠO RA KẾT QUẢ DÙNG ĐƯỢC. Một lượt
 * chấm hỏng không tạo ra gì nên không có gì để thu — nhưng lượt chấm THÀNH CÔNG trước đó thì có, và
 * không được hoàn lại.
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
        repository.markWaivedByExamSessionId(sessionId, Instant.now());

        insertUsage(sessionId, "100000"); // regrade attempt
        Instant chargedAt = Instant.now();
        int claimed = repository.markChargedByExamSessionId(sessionId, chargedAt);

        assertThat(claimed).isEqualTo(1);
        assertThat(repository.sumCostVndByExamSessionIdAndChargedAt(sessionId, chargedAt))
            .isEqualByComparingTo("100000");
        assertThat(repository.sumWaivedCostVndByExamSessionId(sessionId)).isEqualByComparingTo("40000");
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
        repository.markChargedByExamSessionId(sessionId, chargedAt);

        insertUsage(sessionId, "40000");
        int waived = repository.markWaivedByExamSessionId(sessionId, Instant.now());

        assertThat(waived).isEqualTo(1);
        assertThat(repository.sumCostVndByExamSessionIdAndChargedAt(sessionId, chargedAt))
            .isEqualByComparingTo("100000");
        assertThat(repository.sumWaivedCostVndByExamSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /**
     * Waiving twice does not double up: the second time there is no unsettled row left to touch.
     */
    @Test
    void shouldBeIdempotentAcrossRepeatedWaives() {
        UUID sessionId = UUID.randomUUID();
        insertUsage(sessionId, "40000");

        assertThat(repository.markWaivedByExamSessionId(sessionId, Instant.now())).isEqualTo(1);
        assertThat(repository.markWaivedByExamSessionId(sessionId, Instant.now())).isZero();
        assertThat(repository.sumWaivedCostVndByExamSessionId(sessionId)).isEqualByComparingTo("40000");
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
        repository.markWaivedByExamSessionId(sessionId, Instant.now());

        assertThat(repository.markChargedByExamSessionId(sessionId, Instant.now())).isZero();
    }

    private void insertUsage(UUID examSessionId, String costVnd) {
        em.createNativeQuery("""
            INSERT INTO ai_usage_records (id, exam_session_id, turn_id, usage_event_id, usage_type, provider,
                                          unit_price_json, cost_usd, cost_vnd, fx_rate_used, occurred_at)
            VALUES (:id, :sessionId, :turnId, :eventId, 'LLM_TOKEN', 'openai',
                    '{}', 1.0, :costVnd, 25000, :now)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("sessionId", examSessionId)
            .setParameter("turnId", UUID.randomUUID())
            .setParameter("eventId", UUID.randomUUID())
            .setParameter("costVnd", new BigDecimal(costVnd))
            .setParameter("now", Instant.now())
            .executeUpdate();
        em.flush();
        em.clear();
    }
}
