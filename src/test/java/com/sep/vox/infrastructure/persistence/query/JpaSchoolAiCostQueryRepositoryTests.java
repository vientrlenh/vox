package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.config.ContainerTestConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT: cả hai câu đều dựa vào thứ không mock được — {@code date_trunc} trên múi giờ nghiệp
 * vụ, {@code COALESCE(SUM(...))} trên tập rỗng, và một {@code LEFT JOIN} phải không được nhân đôi
 * tổng tiền.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaSchoolAiCostQueryRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaSchoolAiCostQueryRepositoryTests extends ContainerTestConfig {

    private static final Instant FROM = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private SchoolAiCostQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * CÁI BẪY BẢN CŨ MẮC PHẢI: cắt mốc theo UTC.
     *
     * <p>18:00Z ngày 27/08 là 01:00 SÁNG ngày 28/08 giờ Việt Nam. Cắt theo UTC sẽ dồn nó vào cột
     * 27/08, tức mọi khoản phát sinh từ 00:00 tới 07:00 giờ Việt Nam đều bị đẩy sang ngày hôm trước
     * và cột cuối biểu đồ luôn thiếu một mẩu.
     */
    @Test
    void shouldBucketByVietnameseCalendarDayNotUtc() {
        var school = insertSchool();
        insertExamSpend(school, Instant.parse("2026-08-27T18:00:00Z"), "40000", null);

        var buckets = repository.findBucketedCost(school, FROM, TO, "day");

        assertThat(buckets).hasSize(1);
        // 00:00 ngày 28/08 giờ Việt Nam = 17:00Z ngày 27/08.
        assertThat(buckets.get(0).bucket()).isEqualTo(Instant.parse("2026-08-27T17:00:00Z"));
        assertThat(buckets.get(0).costVnd()).isEqualByComparingTo("40000");
    }

    /** Hai loại ví là hai đường riêng trên biểu đồ, nên không được gộp vào một mốc. */
    @Test
    void shouldKeepEachQuotaTypeAsItsOwnSeries() {
        var school = insertSchool();
        var sameDay = Instant.parse("2026-08-20T03:00:00Z");
        insertExamSpend(school, sameDay, "100000", null);
        insertPracticeSpend(school, sameDay, "7000", insertUser("Nguyễn Minh Anh"));

        var buckets = repository.findBucketedCost(school, FROM, TO, "day");

        assertThat(buckets).hasSize(2);
        assertThat(buckets).anySatisfy(bucket -> {
            assertThat(bucket.quotaType()).isEqualTo("EXAM");
            assertThat(bucket.costVnd()).isEqualByComparingTo("100000");
        });
        assertThat(buckets).anySatisfy(bucket -> {
            assertThat(bucket.quotaType()).isEqualTo("PRACTICE");
            assertThat(bucket.costVnd()).isEqualByComparingTo("7000");
        });
    }

    /** Cửa sổ NỬA MỞ: hai dải liền nhau không được đếm trùng khoản rơi đúng ranh giới. */
    @Test
    void shouldUseAHalfOpenWindow() {
        var school = insertSchool();
        insertExamSpend(school, FROM.minusSeconds(1), "1000", null);
        insertExamSpend(school, FROM, "2000", null);
        insertExamSpend(school, TO, "4000", null);

        var buckets = repository.findBucketedCost(school, FROM, TO, "day");

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).costVnd()).isEqualByComparingTo("2000");
    }

    @Test
    void shouldRankUsersByHowMuchTheySpent() {
        var school = insertSchool();
        var heavy = insertUser("Phạm Thu Hằng");
        var light = insertUser("Lê Quang Vinh");
        insertExamSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "900000", heavy);
        insertExamSpend(school, Instant.parse("2026-08-12T03:00:00Z"), "1010000", heavy);
        insertExamSpend(school, Instant.parse("2026-08-11T03:00:00Z"), "840000", light);

        var page = repository.findSpendByUser(school, FROM, TO, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(2L);
        assertThat(page.content().get(0).fullName()).isEqualTo("Phạm Thu Hằng");
        assertThat(page.content().get(0).spentVnd()).isEqualByComparingTo("1910000");
        assertThat(page.content().get(1).spentVnd()).isEqualByComparingTo("840000");
    }

    /**
     * Khoản của kỳ thi tập trung mang user_id null và phải nằm NGOÀI bảng xếp hạng — ở gần như mọi
     * trường nó là khoản lớn nhất và sẽ chiếm đỉnh bảng, che mất đúng thứ bảng này sinh ra để thấy.
     */
    @Test
    void shouldKeepSchoolWideSpendingOutOfTheUserRanking() {
        var school = insertSchool();
        var teacher = insertUser("Phạm Thu Hằng");
        insertExamSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "5000000", null);
        insertExamSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "300000", teacher);

        var page = repository.findSpendByUser(school, FROM, TO, null, 1, 20);

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
     * {@code LEFT JOIN} sang trần chi KHÔNG được nhân đôi tổng tiền — khoá duy nhất của bảng đó đảm
     * bảo tối đa một dòng mỗi bút toán, và test này canh đúng giả định ấy.
     */
    @Test
    void shouldAttachThePersonalCapWithoutInflatingTheTotal() {
        var school = insertSchool();
        var teacher = insertUser("Phạm Thu Hằng");
        var subscriptionId = UUID.randomUUID();
        insertExamSpend(school, subscriptionId, Instant.parse("2026-08-10T03:00:00Z"), "700000", teacher);
        insertExamSpend(school, subscriptionId, Instant.parse("2026-08-11T03:00:00Z"), "500000", teacher);
        insertAllocation(subscriptionId, "EXAM", teacher, "2000000");

        var row = repository.findSpendByUser(school, FROM, TO, null, 1, 20).content().get(0);

        assertThat(row.spentVnd()).isEqualByComparingTo("1200000");
        assertThat(row.allocatedAmountVnd()).isEqualByComparingTo("2000000");
    }

    /** Không có trần chi vẫn phải hiện: người đó vẫn tiêu tiền của trường, chỉ là không có mức để so. */
    @Test
    void shouldListAUserWhoHasNoPersonalCap() {
        var school = insertSchool();
        insertExamSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "700000", insertUser("Lê Quang Vinh"));

        var row = repository.findSpendByUser(school, FROM, TO, null, 1, 20).content().get(0);

        assertThat(row.spentVnd()).isEqualByComparingTo("700000");
        assertThat(row.allocatedAmountVnd()).isNull();
    }

    /** Một người tiêu ở cả hai ví là HAI hàng — trần chi cũng chia theo loại ví. */
    @Test
    void shouldSplitOneUserAcrossBothQuotaTypes() {
        var school = insertSchool();
        var user = insertUser("Nguyễn Minh Anh");
        insertExamSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "700000", user);
        insertPracticeSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "50000", user);

        assertThat(repository.findSpendByUser(school, FROM, TO, null, 1, 20).totalElements()).isEqualTo(2L);
        assertThat(repository.findSpendByUser(school, FROM, TO, "PRACTICE", 1, 20).totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldReportWhenTheLedgerStartedForThisSchool() {
        var school = insertSchool();
        insertExamSpend(school, Instant.parse("2026-08-19T03:00:00Z"), "1000", null);
        insertExamSpend(school, Instant.parse("2026-08-04T03:00:00Z"), "1000", null);

        assertThat(repository.findFirstRecordedAt(school)).isEqualTo(Instant.parse("2026-08-04T03:00:00Z"));
        assertThat(repository.findFirstRecordedAt(insertSchool())).isNull();
    }

    @Test
    void shouldNotLeakSpendingFromAnotherSchool() {
        var school = insertSchool();
        insertExamSpend(school, Instant.parse("2026-08-10T03:00:00Z"), "1000", null);
        insertExamSpend(insertSchool(), Instant.parse("2026-08-10T03:00:00Z"), "9000", null);

        assertThat(repository.findBucketedCost(school, FROM, TO, "day").get(0).costVnd())
            .isEqualByComparingTo("1000");
    }

    // ---------- fixtures ----------

    private UUID insertSchool() {
        var id = UUID.randomUUID();
        var actor = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
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
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
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

    private void insertExamSpend(UUID schoolId, Instant occurredAt, String amountVnd, UUID userId) {
        insertExamSpend(schoolId, UUID.randomUUID(), occurredAt, amountVnd, userId);
    }

    private void insertExamSpend(UUID schoolId, UUID subscriptionId, Instant occurredAt,
            String amountVnd, UUID userId) {
        insertSpend(schoolId, subscriptionId, "EXAM", occurredAt, amountVnd, userId,
            UUID.randomUUID(), null);
    }

    private void insertPracticeSpend(UUID schoolId, Instant occurredAt, String amountVnd, UUID userId) {
        insertSpend(schoolId, UUID.randomUUID(), "PRACTICE", occurredAt, amountVnd, userId,
            null, UUID.randomUUID());
    }

    private void insertSpend(UUID schoolId, UUID subscriptionId, String quotaType, Instant occurredAt,
            String amountVnd, UUID userId, UUID examSessionId, UUID practiceSessionId) {
        em.createNativeQuery("""
            INSERT INTO school_ai_spend_entries (id, school_id, subscription_id, quota_type, user_id,
                                                 exam_session_id, practice_session_id, amount_vnd, occurred_at)
            VALUES (:id, :schoolId, :subscriptionId, :quotaType, :userId,
                    :examSessionId, :practiceSessionId, :amountVnd, :occurredAt)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("subscriptionId", subscriptionId)
            .setParameter("quotaType", quotaType)
            .setParameter("userId", userId)
            .setParameter("examSessionId", examSessionId)
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
