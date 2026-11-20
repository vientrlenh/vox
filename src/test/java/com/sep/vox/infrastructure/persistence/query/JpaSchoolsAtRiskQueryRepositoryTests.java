package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.application.query.repository.SchoolsAtRiskQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT trên Postgres thật.
 *
 * <p>Điều đáng kiểm nhất KHÔNG phải "danh sách có trả về dòng không", mà là danh sách có ĐẾM RA ĐÚNG
 * con số mà thẻ trên trang tổng quan đang hiện hay không. Vì thế phần lớn test ở đây gọi CẢ HAI
 * repository trên cùng một tập dữ liệu và so hai kết quả — đó là lỗi mà bản thiết kế cảnh báo: bấm
 * vào thẻ ghi 5 rồi thấy 9 dòng.
 */
@RepositoryTest
@Import({ JpaSchoolsAtRiskQueryRepository.class, JpaPlatformBusinessHealthQueryRepository.class })
class JpaSchoolsAtRiskQueryRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-08-30T05:00:00Z");
    private static final Instant EXPIRING_THROUGH = NOW.plus(30, ChronoUnit.DAYS);

    @Autowired
    private SchoolsAtRiskQueryRepository repository;

    @Autowired
    private PlatformBusinessHealthQueryRepository countRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * The main trap of this screen: a school with an expired 2024 period AND a running 2025 period
     * is placed by the "Schools & plans" screen in the status=EXPIRED list, but it does NOT belong
     * to the "expired" group -- because that group counts by SCHOOL, not by subscription row.
     */
    @Test
    void shouldNotListASchoolAsLapsedWhenAnOlderTermExpiredButANewerOneCovers() {
        UUID school = insertSchool("THPT Nguyễn Huệ");
        insertSubscription(school, "EXPIRED", NOW.minus(700, ChronoUnit.DAYS), NOW.minus(335, ChronoUnit.DAYS));
        insertSubscription(school, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(65, ChronoUnit.DAYS));

        PageResult<SchoolAtRiskDto> lapsed = repository.findByBucket(SchoolRiskBucket.LAPSED, NOW, EXPIRING_THROUGH, null, 1, 20);

        assertThat(lapsed.totalElements()).isZero();
        assertThat(countRepository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH).lapsedSchools()).isZero();
    }

    /**
     * The four groups must count EXACTLY the numbers on the card of the overview page -- the same
     * predicate, the same boundary.
     */
    @Test
    void shouldMatchTheDashboardCountForEveryBucket() {
        UUID expiring = insertSchool("Sắp hết hạn");
        insertSubscription(expiring, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS));

        UUID lapsed = insertSchool("Đã hết hạn");
        insertSubscription(lapsed, "EXPIRED", NOW.minus(400, ChronoUnit.DAYS), NOW.minus(35, ChronoUnit.DAYS));

        UUID suspended = insertSchool("Bị đình chỉ");
        insertSuspendedSubscription(suspended, NOW.minus(300, ChronoUnit.DAYS), NOW.plus(65, ChronoUnit.DAYS),
            "Vi phạm điều khoản sử dụng");

        UUID indebt = insertSchool("Đang nợ");
        insertSubscription(indebt, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(200, ChronoUnit.DAYS));
        insertBalance(indebt, new BigDecimal("-2450000"));

        SchoolSubscriptionHealthDto health = countRepository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH);

        assertThat(repository.findByBucket(SchoolRiskBucket.EXPIRING_SOON, NOW, EXPIRING_THROUGH, null, 1, 20)
            .totalElements()).isEqualTo(health.expiringSoonSchools());
        assertThat(repository.findByBucket(SchoolRiskBucket.LAPSED, NOW, EXPIRING_THROUGH, null, 1, 20)
            .totalElements()).isEqualTo(health.lapsedSchools());
        assertThat(repository.findByBucket(SchoolRiskBucket.SUSPENDED, NOW, EXPIRING_THROUGH, null, 1, 20)
            .totalElements()).isEqualTo(health.suspendedSchools());
        assertThat(repository.findByBucket(SchoolRiskBucket.IN_DEBT, NOW, EXPIRING_THROUGH, null, 1, 20)
            .totalElements()).isEqualTo(countRepository.countSchoolsInDebt());
    }

    /**
     * "In debt" CUTS ACROSS the other three groups: it reads from the wallet, not from the
     * subscription status. A school that still has a plan but a negative wallet must appear in BOTH
     * lists.
     */
    @Test
    void shouldListASubscribedSchoolInDebtInBothBuckets() {
        UUID school = insertSchool("THPT Nguyễn Huệ");
        insertSubscription(school, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS));
        insertBalance(school, new BigDecimal("-2450000"));

        assertThat(repository.findByBucket(SchoolRiskBucket.EXPIRING_SOON, NOW, EXPIRING_THROUGH, null, 1, 20)
            .totalElements()).isEqualTo(1L);

        PageResult<SchoolAtRiskDto> debt = repository.findByBucket(SchoolRiskBucket.IN_DEBT, NOW, EXPIRING_THROUGH, null, 1, 20);
        assertThat(debt.totalElements()).isEqualTo(1L);
        assertThat(debt.content().get(0).balanceVnd()).isEqualByComparingTo("-2450000");
    }

    /**
     * The plan name and the expiry date must belong to the SAME period -- the period that covers
     * the earliest end, exactly the period {@code MIN(end_date)} uses to decide "about to expire".
     * Taking one period's plan name and pairing it with another period's date would make the screen
     * say something untrue.
     */
    @Test
    void shouldTakeThePlanNameAndEndDateFromTheSameCoveringTerm() {
        UUID school = insertSchool("THPT Nguyễn Huệ");
        insertSubscription(school, "EXPIRED", NOW.minus(700, ChronoUnit.DAYS), NOW.minus(335, ChronoUnit.DAYS),
            insertPlan("Gói Cơ bản"));
        insertSubscription(school, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS),
            insertPlan("Gói Chuẩn"));

        PageResult<SchoolAtRiskDto> page = repository.findByBucket(SchoolRiskBucket.EXPIRING_SOON, NOW, EXPIRING_THROUGH, null, 1, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).planName()).isEqualTo("Gói Chuẩn");
        assertThat(page.content().get(0).relevantEndDate()).isEqualTo(NOW.plus(10, ChronoUnit.DAYS));
    }

    /**
     * The suspension reason is the most valuable information of that group -- if it cannot be
     * carried, the tab is almost useless.
     */
    @Test
    void shouldCarryTheSuspensionReason() {
        UUID school = insertSchool("THPT Trần Phú");
        insertSuspendedSubscription(school, NOW.minus(300, ChronoUnit.DAYS), NOW.plus(65, ChronoUnit.DAYS),
            "Chậm thanh toán quá 60 ngày");

        PageResult<SchoolAtRiskDto> page = repository.findByBucket(SchoolRiskBucket.SUSPENDED, NOW, EXPIRING_THROUGH, null, 1, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).suspendedReason()).isEqualTo("Chậm thanh toán quá 60 ngày");
    }

    /**
     * Trường CHƯA TỪNG mua gói nào không thuộc nhóm nào cả — nó không có dòng nào để gộp. Giữ đúng
     * như phép đếm là có chủ đích: hai màn hình phải nói cùng một con số.
     */
    @Test
    void shouldLeaveOutSchoolsThatNeverHadASubscription() {
        insertSchool("Trường mới duyệt");

        assertThat(repository.findByBucket(SchoolRiskBucket.LAPSED, NOW, EXPIRING_THROUGH, null, 1, 20)
            .totalElements()).isZero();
        assertThat(countRepository.countSchoolSubscriptionHealth(NOW, EXPIRING_THROUGH).lapsedSchools()).isZero();
    }

    /**
     * A school that has never topped up its wallet has a balance of 0, not null -- the same
     * convention as SchoolBalance.emptyFor.
     */
    @Test
    void shouldReportZeroBalanceForSchoolsThatNeverToppedUp() {
        UUID school = insertSchool("THPT Lê Quý Đôn");
        insertSubscription(school, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS));

        PageResult<SchoolAtRiskDto> page = repository.findByBucket(SchoolRiskBucket.EXPIRING_SOON, NOW, EXPIRING_THROUGH, null, 1, 20);

        assertThat(page.content().get(0).balanceVnd()).isEqualByComparingTo("0");
    }

    @Test
    void shouldSearchByNameOrCode() {
        UUID huong = insertSchool("THPT Nguyễn Huệ");
        insertSubscription(huong, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS));
        UUID phu = insertSchool("THPT Trần Phú");
        insertSubscription(phu, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS));

        PageResult<SchoolAtRiskDto> byName = repository.findByBucket(SchoolRiskBucket.EXPIRING_SOON, NOW, EXPIRING_THROUGH, "trần", 1, 20);

        assertThat(byName.totalElements()).isEqualTo(1L);
        assertThat(byName.content().get(0).schoolName()).isEqualTo("THPT Trần Phú");
    }

    /**
     * About-to-expire is ordered by the NEAREST expiry date first -- that is the order in which the
     * operator makes calls.
     */
    @Test
    void shouldOrderExpiringSoonByNearestEndDateFirst() {
        UUID later = insertSchool("Rụng sau");
        insertSubscription(later, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(25, ChronoUnit.DAYS));
        UUID sooner = insertSchool("Rụng trước");
        insertSubscription(sooner, "ACTIVE", NOW.minus(300, ChronoUnit.DAYS), NOW.plus(3, ChronoUnit.DAYS));

        PageResult<SchoolAtRiskDto> page = repository.findByBucket(SchoolRiskBucket.EXPIRING_SOON, NOW, EXPIRING_THROUGH, null, 1, 20);

        assertThat(page.content()).extracting(dto -> dto.schoolName())
            .containsExactly("Rụng trước", "Rụng sau");
    }

    // ---------- fixtures ----------

    private UUID insertSchool(String name) {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO schools (id, code, name, address, contact_email, contact_phone,
                                 is_active, student_count, created_at, updated_at, created_by, updated_by)
            VALUES (:id, :code, :name, 'So 1', 'a@b.vn', '0900000000',
                    TRUE, 10, :now, :now, :actor, :actor)
            """)
            .setParameter("id", id)
            .setParameter("code", "C-" + id.toString().substring(0, 8))
            .setParameter("name", name)
            .setParameter("now", now)
            .setParameter("actor", actor)
            .executeUpdate();
        return id;
    }

    private UUID insertPlan(String name) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO subscription_plans (id, name, tagline, status, price_vnd, period_type, period_count,
                                            version, max_time_per_attempt_min, created_at, updated_at)
            VALUES (:id, :name, '', 'ACTIVE', 36000000, 'DAY', 365, 1, 30, :now, :now)
            """)
            .setParameter("id", id)
            .setParameter("name", name)
            .setParameter("now", now)
            .executeUpdate();
        return id;
    }

    private void insertSubscription(UUID schoolId, String status, Instant startDate, Instant endDate) {
        insertSubscription(schoolId, status, startDate, endDate, insertPlan("Gói Chuẩn"));
    }

    private void insertSubscription(UUID schoolId, String status, Instant startDate, Instant endDate, UUID planId) {
        em.createNativeQuery("""
            INSERT INTO school_subscriptions (id, school_id, subscription_plan_id, status, start_date, end_date,
                                              price_paid_snapshot, created_at, version)
            VALUES (:id, :schoolId, :planId, :status, :startDate, :endDate, 1000000, :now, 0)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("planId", planId)
            .setParameter("status", status)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("now", OffsetDateTime.now(ZoneOffset.UTC))
            .executeUpdate();
    }

    private void insertSuspendedSubscription(UUID schoolId, Instant startDate, Instant endDate, String reason) {
        em.createNativeQuery("""
            INSERT INTO school_subscriptions (id, school_id, subscription_plan_id, status, start_date, end_date,
                                              price_paid_snapshot, created_at, version,
                                              suspended_at, suspended_reason)
            VALUES (:id, :schoolId, :planId, 'SUSPENDED', :startDate, :endDate, 1000000, :now, 0,
                    :now, :reason)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("planId", insertPlan("Gói Chuẩn"))
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("now", OffsetDateTime.now(ZoneOffset.UTC))
            .setParameter("reason", reason)
            .executeUpdate();
    }

    private void insertBalance(UUID schoolId, BigDecimal balanceVnd) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO school_balances (id, school_id, balance_vnd, created_at, updated_at, version)
            VALUES (:id, :schoolId, :balance, :now, :now, 0)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("balance", balanceVnd)
            .setParameter("now", now)
            .executeUpdate();
    }
}
