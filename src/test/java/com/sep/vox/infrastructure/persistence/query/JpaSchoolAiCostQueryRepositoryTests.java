package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.application.query.dto.AiCostBucketDto;
import com.sep.vox.application.query.dto.UserAiSpendDto;
import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT: cả hai câu đều dựa vào thứ không mock được — {@code date_trunc} trên múi giờ nghiệp
 * vụ, {@code COALESCE(SUM(...))} trên tập rỗng, và một {@code LEFT JOIN} phải không được nhân đôi
 * tổng tiền.
 */
@RepositoryTest
@Import(JpaSchoolAiCostQueryRepository.class)
class JpaSchoolAiCostQueryRepositoryTests {

    private static final Instant FROM = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private SchoolAiCostQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * THE TRAP THE OLD VERSION FELL INTO: cutting the boundary by UTC.
     *
     * <p>18:00Z on 27/08 is 01:00 in the MORNING of 28/08 Vietnam time. Cutting by UTC would pile
     * it into the 27/08 column, so every charge between 00:00 and 07:00 Vietnam time is pushed to
     * the previous day and the last column of the chart is always missing a piece.
     */
    @Test
    void shouldBucketByVietnameseCalendarDayNotUtc() {
        UUID school = insertSchool();
        insertAssessmentSpend(school, Instant.parse("2026-08-27T18:00:00Z"), "40000", null);

        List<AiCostBucketDto> buckets = repository.findBucketedCost(school, FROM, TO, "day");

        assertThat(buckets).hasSize(1);
        // 00:00 on 28/08 Vietnam time = 17:00Z on 27/08.
        assertThat(buckets.get(0).bucket()).isEqualTo(Instant.parse("2026-08-27T17:00:00Z"));
        assertThat(buckets.get(0).costVnd()).isEqualByComparingTo("40000");
    }

    /**
     * The two wallet types are two separate lines on the chart, so they must not be merged into one
     * point.
     */
    @Test
    void shouldKeepEachQuotaTypeAsItsOwnSeries() {
        UUID school = insertSchool();
        Instant sameDay = Instant.parse("2026-08-20T03:00:00Z");
        insertAssessmentSpend(school, sameDay, "100000", null);
        insertPracticeSpend(school, sameDay, "7000", insertUser("Nguyễn Minh Anh"));

        List<AiCostBucketDto> buckets = repository.findBucketedCost(school, FROM, TO, "day");

        assertThat(buckets).hasSize(2);
        assertThat(buckets).anySatisfy(bucket -> {
            assertThat(bucket.quotaType()).isEqualTo("ASSESSMENT");
            assertThat(bucket.costVnd()).isEqualByComparingTo("100000");
        });
        assertThat(buckets).anySatisfy(bucket -> {
            assertThat(bucket.quotaType()).isEqualTo("PRACTICE");
            assertThat(bucket.costVnd()).isEqualByComparingTo("7000");
        });
    }

    /**
     * A HALF-OPEN window: two adjacent ranges must not double-count an amount that falls exactly on
     * the boundary.
     */
    @Test
    void shouldUseAHalfOpenWindow() {
        UUID school = insertSchool();
        insertAssessmentSpend(school, FROM.minusSeconds(1), "1000", null);
        insertAssessmentSpend(school, FROM, "2000", null);
        insertAssessmentSpend(school, TO, "4000", null);

        List<AiCostBucketDto> buckets = repository.findBucketedCost(school, FROM, TO, "day");

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).costVnd()).isEqualByComparingTo("2000");
    }

    @Test
    void shouldRankUsersByHowMuchTheySpent() {
        UUID school = insertSchool();
        UUID heavy = insertUser("Phạm Thu Hằng");
        UUID light = insertUser("Lê Quang Vinh");
        insertAssessmentSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "900000", heavy);
        insertAssessmentSpend(school, Instant.parse("2026-08-12T03:00:00Z"), "1010000", heavy);
        insertAssessmentSpend(school, Instant.parse("2026-08-11T03:00:00Z"), "840000", light);

        PageResult<UserAiSpendDto> page = repository.findSpendByUser(school, FROM, TO, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.content().get(0).fullName()).isEqualTo("Phạm Thu Hằng");
        assertThat(page.content().get(0).spentVnd()).isEqualByComparingTo("1910000");
        assertThat(page.content().get(1).spentVnd()).isEqualByComparingTo("840000");
    }

    /**
     * A charge from a centralized assessment carries a null user_id and must stay OUTSIDE the ranking --
     * in almost every school it is the largest item and would take the top of the table, hiding
     * exactly what this table exists to show.
     */
    @Test
    void shouldKeepSchoolWideSpendingOutOfTheUserRanking() {
        UUID school = insertSchool();
        UUID teacher = insertUser("Phạm Thu Hằng");
        insertAssessmentSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "5000000", null);
        insertAssessmentSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "300000", teacher);

        PageResult<UserAiSpendDto> page = repository.findSpendByUser(school, FROM, TO, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.content().get(0).spentVnd()).isEqualByComparingTo("300000");
        assertThat(repository.sumSchoolWideCost(school, FROM, TO, null)).isEqualByComparingTo("5000000");
    }

    /** Trường chưa tiêu gì thì tổng phải là 0, không phải null làm hỏng một field non-null. */
    @Test
    void shouldReturnZeroSchoolWideSpendingWhenNothingWasSpent() {
        assertThat(repository.sumSchoolWideCost(insertSchool(), FROM, TO, null))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * The {@code LEFT JOIN} to the spending ceiling must NOT double the total -- the unique key of
     * that table guarantees at most one row per ledger entry, and this test guards exactly that
     * assumption.
     */
    @Test
    void shouldAttachThePersonalCapWithoutInflatingTheTotal() {
        UUID school = insertSchool();
        UUID teacher = insertUser("Phạm Thu Hằng");
        UUID subscriptionId = UUID.randomUUID();
        insertAssessmentSpend(school, subscriptionId, Instant.parse("2026-08-10T03:00:00Z"), "700000", teacher);
        insertAssessmentSpend(school, subscriptionId, Instant.parse("2026-08-11T03:00:00Z"), "500000", teacher);
        insertAllocation(subscriptionId, "ASSESSMENT", teacher, "2000000");

        UserAiSpendDto row = repository.findSpendByUser(school, FROM, TO, null, 1, 20).content().get(0);

        assertThat(row.spentVnd()).isEqualByComparingTo("1200000");
        assertThat(row.allocatedAmountVnd()).isEqualByComparingTo("2000000");
    }

    /**
     * Having no spending ceiling must still show up: that person still spends the school's money,
     * there is just no level to compare against.
     */
    @Test
    void shouldListAUserWhoHasNoPersonalCap() {
        UUID school = insertSchool();
        insertAssessmentSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "700000", insertUser("Lê Quang Vinh"));

        UserAiSpendDto row = repository.findSpendByUser(school, FROM, TO, null, 1, 20).content().get(0);

        assertThat(row.spentVnd()).isEqualByComparingTo("700000");
        assertThat(row.allocatedAmountVnd()).isNull();
    }

    /**
     * A person who spends in both wallets is TWO rows -- the spending ceiling is also split by
     * wallet type.
     */
    @Test
    void shouldSplitOneUserAcrossBothQuotaTypes() {
        UUID school = insertSchool();
        UUID user = insertUser("Nguyễn Minh Anh");
        insertAssessmentSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "700000", user);
        insertPracticeSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "50000", user);

        assertThat(repository.findSpendByUser(school, FROM, TO, null, 1, 20).totalElements()).isEqualTo(2L);
        assertThat(repository.findSpendByUser(school, FROM, TO, "PRACTICE", 1, 20).totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldReportWhenTheLedgerStartedForThisSchool() {
        UUID school = insertSchool();
        insertAssessmentSpend(school, Instant.parse("2026-08-19T03:00:00Z"), "1000", null);
        insertAssessmentSpend(school, Instant.parse("2026-08-04T03:00:00Z"), "1000", null);

        assertThat(repository.findFirstRecordedAt(school)).isEqualTo(Instant.parse("2026-08-04T03:00:00Z"));
        assertThat(repository.findFirstRecordedAt(insertSchool())).isNull();
    }

    @Test
    void shouldNotLeakSpendingFromAnotherSchool() {
        UUID school = insertSchool();
        insertAssessmentSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "1000", null);
        insertAssessmentSpend(insertSchool(), Instant.parse("2026-08-10T03:00:00Z"), "9000", null);

        assertThat(repository.findBucketedCost(school, FROM, TO, "day").get(0).costVnd())
            .isEqualByComparingTo("1000");
    }

    // ---------- fixtures ----------

    private UUID insertSchool() {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO schools (id, code, name, address, contact_email, contact_phone,
                                 is_active, student_count, created_at, updated_at, created_by, updated_by)
            VALUES (:id, :code, 'THPT Nguyễn Trãi', 'So 1', 'a@b.vn', '0900000000',
                    TRUE, 10, :now, :now, :actor, :actor)
            """)
            .setParameter("id", id)
            .setParameter("code", "C-" + id.toString().substring(0, 8))
            .setParameter("now", now)
            .setParameter("actor", actor)
            .executeUpdate();
        return id;
    }

    private UUID insertUser(String fullName) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO users (id, email, full_name, password_hash, date_of_birth, status, created_at, updated_at)
            VALUES (:id, :email, :fullName, 'x', DATE '1990-01-01', 'ACTIVE', :now, :now)
            """)
            .setParameter("id", id)
            .setParameter("email", id + "@vox.test")
            .setParameter("fullName", fullName)
            .setParameter("now", now)
            .executeUpdate();
        return id;
    }

    private void insertAssessmentSpend(UUID schoolId, Instant occurredAt, String amountVnd, UUID userId) {
        insertAssessmentSpend(schoolId, UUID.randomUUID(), occurredAt, amountVnd, userId);
    }

    private void insertAssessmentSpend(UUID schoolId, UUID subscriptionId, Instant occurredAt,
            String amountVnd, UUID userId) {
        insertSpend(schoolId, subscriptionId, "ASSESSMENT", occurredAt, amountVnd, userId,
            UUID.randomUUID(), null);
    }

    private void insertPracticeSpend(UUID schoolId, Instant occurredAt, String amountVnd, UUID userId) {
        insertSpend(schoolId, UUID.randomUUID(), "PRACTICE", occurredAt, amountVnd, userId,
            null, UUID.randomUUID());
    }

    private void insertSpend(UUID schoolId, UUID subscriptionId, String quotaType, Instant occurredAt,
            String amountVnd, UUID userId, UUID assessmentSessionId, UUID practiceSessionId) {
        em.createNativeQuery("""
            INSERT INTO school_ai_spend_entries (id, school_id, subscription_id, quota_type, user_id,
                                                 assessment_session_id, practice_session_id, amount_vnd, occurred_at)
            VALUES (:id, :schoolId, :subscriptionId, :quotaType, :userId,
                    :assessmentSessionId, :practiceSessionId, :amountVnd, :occurredAt)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("subscriptionId", subscriptionId)
            .setParameter("quotaType", quotaType)
            .setParameter("userId", userId)
            .setParameter("assessmentSessionId", assessmentSessionId)
            .setParameter("practiceSessionId", practiceSessionId)
            .setParameter("amountVnd", new BigDecimal(amountVnd))
            .setParameter("occurredAt", occurredAt)
            .executeUpdate();
        em.flush();
        em.clear();
    }

    private void insertAllocation(UUID subscriptionId, String quotaType, UUID userId, String allocatedVnd) {
        em.createNativeQuery("""
            INSERT INTO school_subscription_quota_user_allocations
                (id, school_subscription_id, quota_type, user_id, allocated_amount_vnd, used_amount_vnd)
            VALUES (:id, :subscriptionId, :quotaType, :userId, :allocated, 0)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("subscriptionId", subscriptionId)
            .setParameter("quotaType", quotaType)
            .setParameter("userId", userId)
            .setParameter("allocated", new BigDecimal(allocatedVnd))
            .executeUpdate();
        em.flush();
        em.clear();
    }
}
