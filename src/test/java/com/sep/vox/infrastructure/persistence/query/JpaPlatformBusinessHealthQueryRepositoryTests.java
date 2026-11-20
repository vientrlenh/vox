package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT trên Postgres thật — xem lý do ở
 * {@link JpaPlatformOperationalHealthQueryRepositoryTests}.
 *
 * <p>Trọng tâm là phép phân loại TỪNG TRƯỜNG: một trường có nhiều kỳ thuê bao trong lịch sử, nên
 * đếm theo dòng sẽ vừa cộng trùng vừa xếp một trường đang dùng tốt vào nhóm "đã hết hạn" chỉ vì kỳ
 * năm ngoái của nó đã kết thúc. Đó là chỗ dễ sai nhất trong cả hai repository mới.
 */
@RepositoryTest
@Import(JpaPlatformBusinessHealthQueryRepository.class)
class JpaPlatformBusinessHealthQueryRepositoryTests {

    @Autowired
    private PlatformBusinessHealthQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Mốc "bây giờ" cố định. Là {@link Instant} chứ không phải ngày lịch vì V2 đã đổi
     * {@code start_date} / {@code end_date} của {@code school_subscriptions} sang {@code timestamptz}
     * — so ngày với timestamptz sẽ để Postgres ép ngày đó thành nửa đêm theo múi giờ SESSION.
     */
    private static final Instant NOW = Instant.parse("2026-08-30T05:00:00Z");
    private static final Instant EXPIRING_THROUGH = NOW.plus(30, java.time.temporal.ChronoUnit.DAYS);

    private static Instant atVietnamStartOfDay(LocalDate day) {
        return day.atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();
    }

    private UUID insertSchool() {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO schools (id, code, name, address, contact_email, contact_phone,
                                 is_active, student_count, created_at, updated_at, created_by, updated_by)
            VALUES (:id, :code, 'Truong Test', 'So 1', 'a@b.vn', '0900000000',
                    TRUE, 10, :now, :now, :actor, :actor)
            """)
            .setParameter("id", id)
            .setParameter("code", "C-" + id.toString().substring(0, 8))
            .setParameter("now", now)
            .setParameter("actor", actor)
            .executeUpdate();
        return id;
    }

    private void insertSubscription(UUID schoolId, String status, Instant startDate, Instant endDate) {
        em.createNativeQuery("""
            INSERT INTO school_subscriptions (id, school_id, subscription_plan_id, status, start_date, end_date,
                                              price_paid_snapshot, created_at, version)
            VALUES (:id, :schoolId, :planId, :status, :startDate, :endDate, 1000000, :now, 0)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("planId", UUID.randomUUID())
            .setParameter("status", status)
            .setParameter("startDate", OffsetDateTime.ofInstant(startDate, ZoneOffset.UTC))
            .setParameter("endDate", OffsetDateTime.ofInstant(endDate, ZoneOffset.UTC))
            .setParameter("now", OffsetDateTime.now(ZoneOffset.UTC))
            .executeUpdate();
    }

    private void insertBalance(UUID schoolId, String balanceVnd) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO school_balances (id, school_id, balance_vnd, created_at, updated_at, version)
            VALUES (:id, :schoolId, CAST(:balance AS numeric), :now, :now, 0)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("balance", balanceVnd)
            .setParameter("now", now)
            .executeUpdate();
    }

    private void insertAiUsage(Instant occurredAt, String costVnd) {
        em.createNativeQuery("""
            INSERT INTO ai_usage_records (id, assessment_session_id, turn_id, usage_event_id, usage_type,
                                          provider, unit_price_json, cost_usd, cost_vnd, fx_rate_used, occurred_at)
            VALUES (:id, :sessionId, :turnId, :eventId, 'LLM_TOKEN', 'anthropic', '{}', 1.5,
                    CAST(:costVnd AS numeric), 26000, :occurredAt)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("sessionId", UUID.randomUUID())
            .setParameter("turnId", UUID.randomUUID())
            .setParameter("eventId", UUID.randomUUID())
            .setParameter("costVnd", costVnd)
            .setParameter("occurredAt", OffsetDateTime.ofInstant(occurredAt, ZoneOffset.UTC))
            .executeUpdate();
    }

    /**
     * School A has ONE expired period and ONE running period. If counted by row instead of by
     * school, it would be counted as both in use and expired -- two groups that should be mutually
     * exclusive.
     */
    @Test
    void classifiesEachSchoolIntoExactlyOneBucket() {
        UUID schoolA = insertSchool();
        insertSubscription(schoolA, "EXPIRED",
            atVietnamStartOfDay(LocalDate.of(2024, 9, 1)), atVietnamStartOfDay(LocalDate.of(2025, 8, 31)));
        insertSubscription(schoolA, "ACTIVE",
            atVietnamStartOfDay(LocalDate.of(2025, 9, 1)), atVietnamStartOfDay(LocalDate.of(2026, 9, 15)));

        UUID schoolB = insertSchool();
        insertSubscription(schoolB, "EXPIRED",
            atVietnamStartOfDay(LocalDate.of(2024, 9, 1)), atVietnamStartOfDay(LocalDate.of(2025, 8, 31)));

        // CANCELLED only turns off auto-renewal; the school can keep using it until endDate.
        UUID schoolC = insertSchool();
        insertSubscription(schoolC, "CANCELLED",
            atVietnamStartOfDay(LocalDate.of(2025, 9, 1)), atVietnamStartOfDay(LocalDate.of(2026, 12, 31)));

        UUID schoolD = insertSchool();
        insertSubscription(schoolD, "SUSPENDED",
            atVietnamStartOfDay(LocalDate.of(2025, 9, 1)), atVietnamStartOfDay(LocalDate.of(2026, 12, 31)));

        // The next period has been paid for but has NOT reached its run date -> right now there is
        // still no plan.
        UUID schoolE = insertSchool();
        insertSubscription(schoolE, "ACTIVE",
            atVietnamStartOfDay(LocalDate.of(2026, 10, 1)), atVietnamStartOfDay(LocalDate.of(2027, 9, 30)));

        SchoolSubscriptionHealthDto health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isEqualTo(2L);   // A, C
        assertThat(health.expiringSoonSchools()).isEqualTo(1L); // A: expires 15/09, within the 30-day threshold
        assertThat(health.lapsedSchools()).isEqualTo(2L);       // B, E
        assertThat(health.suspendedSchools()).isEqualTo(1L);    // D
    }

    /**
     * A period that expires exactly at the threshold mark must still be flagged -- the threshold is
     * INCLUSIVE.
     */
    @Test
    void expiringSoonBoundaryIsInclusive() {
        Instant start = NOW.minus(180, java.time.temporal.ChronoUnit.DAYS);

        UUID onThreshold = insertSchool();
        insertSubscription(onThreshold, "ACTIVE", start, EXPIRING_THROUGH);

        UUID justOutside = insertSchool();
        insertSubscription(justOutside, "ACTIVE", start, EXPIRING_THROUGH.plusMillis(1));

        SchoolSubscriptionHealthDto health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isEqualTo(2L);
        assertThat(health.expiringSoonSchools()).isEqualTo(1L);
    }

    /**
     * A period that ends exactly at the point under consideration is still in effect; one that ends
     * one tick earlier is not.
     */
    @Test
    void coverageBoundaryIsInclusiveAtBothEnds() {
        Instant start = NOW.minus(180, java.time.temporal.ChronoUnit.DAYS);

        UUID endsExactlyNow = insertSchool();
        insertSubscription(endsExactlyNow, "ACTIVE", start, NOW);

        UUID endedJustBefore = insertSchool();
        insertSubscription(endedJustBefore, "ACTIVE", start, NOW.minusMillis(1));

        UUID startsExactlyNow = insertSchool();
        insertSubscription(startsExactlyNow, "ACTIVE", NOW, NOW.plus(365, java.time.temporal.ChronoUnit.DAYS));

        SchoolSubscriptionHealthDto health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isEqualTo(2L);
        assertThat(health.lapsedSchools()).isEqualTo(1L);
    }

    @Test
    void countsZeroOnEmptyTable() {
        SchoolSubscriptionHealthDto health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isZero();
        assertThat(health.expiringSoonSchools()).isZero();
        assertThat(health.lapsedSchools()).isZero();
        assertThat(health.suspendedSchools()).isZero();
    }

    /** Ví âm CHÍNH LÀ khoản nợ — cột cố ý không có CHECK >= 0, xem SchoolBalance. */
    @Test
    void countsOnlySchoolsWithNegativeBalance() {
        insertBalance(insertSchool(), "-2450000");
        insertBalance(insertSchool(), "-1");
        insertBalance(insertSchool(), "0");
        insertBalance(insertSchool(), "5000000");

        assertThat(repository.countSchoolsInDebt()).isEqualTo(2L);
    }

    @Test
    void sumsAiCostInVndOverHalfOpenWindow() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-01T00:00:00Z");

        insertAiUsage(from, "1000.50");
        insertAiUsage(Instant.parse("2026-08-15T12:00:00Z"), "2000.25");
        // Exactly the last point -> EXCLUDED, because `to` is the open boundary.
        insertAiUsage(to, "9999");
        // Before the first point -> excluded.
        insertAiUsage(from.minusMillis(1), "8888");

        assertThat(repository.sumAiCostVnd(from, to)).isEqualByComparingTo("3000.75");
    }

    /**
     * A period with no records must return 0, not null -- otherwise the profit margin throws an
     * NPE.
     */
    @Test
    void aiCostIsZeroWhenNoRecordsInWindow() {
        BigDecimal result = repository.sumAiCostVnd(
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-09-01T00:00:00Z"));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
