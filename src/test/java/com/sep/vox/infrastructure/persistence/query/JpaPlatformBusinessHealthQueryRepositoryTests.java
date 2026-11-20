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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.common.ZoneConstant;

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
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaPlatformBusinessHealthQueryRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaPlatformBusinessHealthQueryRepositoryTests extends ContainerTestConfig {

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
        var id = UUID.randomUUID();
        var actor = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
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
        var now = OffsetDateTime.now(ZoneOffset.UTC);
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
            INSERT INTO ai_usage_records (id, exam_session_id, turn_id, usage_event_id, usage_type,
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
     * Trường A có MỘT kỳ đã hết hạn và MỘT kỳ đang chạy. Nếu đếm theo dòng thay vì theo trường, nó
     * sẽ vừa được tính là đang dùng, vừa bị tính là đã hết hạn — hai nhóm lẽ ra loại trừ nhau.
     */
    @Test
    void classifiesEachSchoolIntoExactlyOneBucket() {
        var schoolA = insertSchool();
        insertSubscription(schoolA, "EXPIRED",
            atVietnamStartOfDay(LocalDate.of(2024, 9, 1)), atVietnamStartOfDay(LocalDate.of(2025, 8, 31)));
        insertSubscription(schoolA, "ACTIVE",
            atVietnamStartOfDay(LocalDate.of(2025, 9, 1)), atVietnamStartOfDay(LocalDate.of(2026, 9, 15)));

        var schoolB = insertSchool();
        insertSubscription(schoolB, "EXPIRED",
            atVietnamStartOfDay(LocalDate.of(2024, 9, 1)), atVietnamStartOfDay(LocalDate.of(2025, 8, 31)));

        // CANCELLED chỉ tắt gia hạn tự động, trường vẫn dùng được tới hết endDate.
        var schoolC = insertSchool();
        insertSubscription(schoolC, "CANCELLED",
            atVietnamStartOfDay(LocalDate.of(2025, 9, 1)), atVietnamStartOfDay(LocalDate.of(2026, 12, 31)));

        var schoolD = insertSchool();
        insertSubscription(schoolD, "SUSPENDED",
            atVietnamStartOfDay(LocalDate.of(2025, 9, 1)), atVietnamStartOfDay(LocalDate.of(2026, 12, 31)));

        // Đã trả tiền cho kỳ sau nhưng kỳ đó CHƯA tới ngày chạy -> hiện tại vẫn là không có gói.
        var schoolE = insertSchool();
        insertSubscription(schoolE, "ACTIVE",
            atVietnamStartOfDay(LocalDate.of(2026, 10, 1)), atVietnamStartOfDay(LocalDate.of(2027, 9, 30)));

        var health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isEqualTo(2L);   // A, C
        assertThat(health.expiringSoonSchools()).isEqualTo(1L); // A: hết 15/09, trong ngưỡng 30 ngày
        assertThat(health.lapsedSchools()).isEqualTo(2L);       // B, E
        assertThat(health.suspendedSchools()).isEqualTo(1L);    // D
    }

    /** Kỳ hết đúng mốc ngưỡng vẫn phải bị cảnh báo — ngưỡng là BAO GỒM. */
    @Test
    void expiringSoonBoundaryIsInclusive() {
        var start = NOW.minus(180, java.time.temporal.ChronoUnit.DAYS);

        var onThreshold = insertSchool();
        insertSubscription(onThreshold, "ACTIVE", start, EXPIRING_THROUGH);

        var justOutside = insertSchool();
        insertSubscription(justOutside, "ACTIVE", start, EXPIRING_THROUGH.plusMillis(1));

        var health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isEqualTo(2L);
        assertThat(health.expiringSoonSchools()).isEqualTo(1L);
    }

    /** Kỳ hết đúng mốc đang xét vẫn còn hiệu lực; hết trước đó một nhịp thì không. */
    @Test
    void coverageBoundaryIsInclusiveAtBothEnds() {
        var start = NOW.minus(180, java.time.temporal.ChronoUnit.DAYS);

        var endsExactlyNow = insertSchool();
        insertSubscription(endsExactlyNow, "ACTIVE", start, NOW);

        var endedJustBefore = insertSchool();
        insertSubscription(endedJustBefore, "ACTIVE", start, NOW.minusMillis(1));

        var startsExactlyNow = insertSchool();
        insertSubscription(startsExactlyNow, "ACTIVE", NOW, NOW.plus(365, java.time.temporal.ChronoUnit.DAYS));

        var health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(health.subscribedSchools()).isEqualTo(2L);
        assertThat(health.lapsedSchools()).isEqualTo(1L);
    }

    @Test
    void countsZeroOnEmptyTable() {
        var health = repository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

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
        var from = Instant.parse("2026-08-01T00:00:00Z");
        var to = Instant.parse("2026-09-01T00:00:00Z");

        insertAiUsage(from, "1000.50");
        insertAiUsage(Instant.parse("2026-08-15T12:00:00Z"), "2000.25");
        // Đúng mốc cuối -> LOẠI, vì `to` là mốc mở.
        insertAiUsage(to, "9999");
        // Trước mốc đầu -> loại.
        insertAiUsage(from.minusMillis(1), "8888");

        assertThat(repository.sumAiCostVnd(from, to)).isEqualByComparingTo("3000.75");
    }

    /** Kỳ không có bản ghi nào phải trả 0, không phải null — nếu không, biên lợi nhuận nổ NPE. */
    @Test
    void aiCostIsZeroWhenNoRecordsInWindow() {
        var result = repository.sumAiCostVnd(
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-09-01T00:00:00Z"));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
