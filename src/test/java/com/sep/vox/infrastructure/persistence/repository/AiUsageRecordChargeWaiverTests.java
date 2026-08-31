package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT: toàn bộ giá trị của V9 nằm ở chỗ hai câu UPDATE không giẫm lên nhau, và điều đó chỉ
 * kiểm được trên một database thật với ràng buộc thật.
 *
 * <p>Quy tắc đang bảo vệ: trường bị thu tiền cho phần việc AI đã TẠO RA KẾT QUẢ DÙNG ĐƯỢC. Một lượt
 * chấm hỏng không tạo ra gì nên không có gì để thu — nhưng lượt chấm THÀNH CÔNG trước đó thì có, và
 * không được hoàn lại.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiUsageRecordChargeWaiverTests extends ContainerTestConfig {

    @Autowired
    private SpringDataAiUsageRecordRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * CÁI BẪY V9 SINH RA ĐỂ CHẶN: lượt chấm hỏng đốt token, lượt chấm lại thành công gọi
     * markCharged() và trước đây nuốt luôn phần của lượt hỏng — trường trả tiền cho cả hai lần.
     */
    @Test
    void shouldNotChargeTheFailedRoundWhenALaterRetrySucceeds() {
        var sessionId = UUID.randomUUID();
        insertUsage(sessionId, "40000");  // lượt hỏng
        repository.markWaivedByExamSessionId(sessionId, Instant.now());

        insertUsage(sessionId, "100000"); // lượt chấm lại
        var chargedAt = Instant.now();
        var claimed = repository.markChargedByExamSessionId(sessionId, chargedAt);

        assertThat(claimed).isEqualTo(1);
        assertThat(repository.sumCostVndByExamSessionIdAndChargedAt(sessionId, chargedAt))
            .isEqualByComparingTo("100000");
        assertThat(repository.sumWaivedCostVndByExamSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /**
     * Chiều ngược lại: miễn KHÔNG được đụng vào dòng đã thu. Một phiên chấm đúng rồi mới hỏng ở lượt
     * sau — miễn đè lên lượt đầu là hoàn tiền cho phần việc đã giao đủ.
     */
    @Test
    void shouldNotWaiveCostThatWasAlreadyCharged() {
        var sessionId = UUID.randomUUID();
        insertUsage(sessionId, "100000");
        var chargedAt = Instant.now();
        repository.markChargedByExamSessionId(sessionId, chargedAt);

        insertUsage(sessionId, "40000");
        var waived = repository.markWaivedByExamSessionId(sessionId, Instant.now());

        assertThat(waived).isEqualTo(1);
        assertThat(repository.sumCostVndByExamSessionIdAndChargedAt(sessionId, chargedAt))
            .isEqualByComparingTo("100000");
        assertThat(repository.sumWaivedCostVndByExamSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /** Miễn hai lần không nhân đôi: lần sau không còn dòng nào chưa ngã ngũ để động vào. */
    @Test
    void shouldBeIdempotentAcrossRepeatedWaives() {
        var sessionId = UUID.randomUUID();
        insertUsage(sessionId, "40000");

        assertThat(repository.markWaivedByExamSessionId(sessionId, Instant.now())).isEqualTo(1);
        assertThat(repository.markWaivedByExamSessionId(sessionId, Instant.now())).isZero();
        assertThat(repository.sumWaivedCostVndByExamSessionId(sessionId)).isEqualByComparingTo("40000");
    }

    /**
     * Phiên hỏng mà KHÔNG ai chấm lại vẫn phải miễn. Trước V9 hành vi phụ thuộc vào việc có người đi
     * khắc phục hay không — bỏ mặc thì miễn phí, đi sửa thì bị tính tiền cho cả lượt hỏng.
     */
    @Test
    void shouldWaiveEvenWhenNobodyEverRetries() {
        var sessionId = UUID.randomUUID();
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
