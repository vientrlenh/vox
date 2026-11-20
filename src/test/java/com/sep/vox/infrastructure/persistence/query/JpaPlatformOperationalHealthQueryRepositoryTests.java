package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;
import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.common.ZoneConstant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT trên Postgres thật. Test unit của use case mock repository nên chứng minh được phần
 * dựng chuỗi ngày, nhưng KHÔNG chứng minh được gì về bản thân câu SQL: {@code COUNT(*) FILTER},
 * {@code AT TIME ZONE}, và cách Hibernate trả cột {@code date} lẫn giá trị {@code COUNT} về Java đều
 * chỉ lộ ra khi có DB thật — đúng loại bẫy đã gây ClassCastException ở
 * {@code JpaTokenUsageTimeseriesQueryRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaPlatformOperationalHealthQueryRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaPlatformOperationalHealthQueryRepositoryTests extends ContainerTestConfig {

    @Autowired
    private PlatformOperationalHealthQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    private static final Instant WINDOW_FROM = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant WINDOW_TO = Instant.parse("2026-08-20T00:00:00Z");

    private void insertSession(UUID examId, String status, Instant startedAt, Instant submittedAt) {
        em.createNativeQuery("""
            INSERT INTO exam_sessions (id, exam_id, candidate_id, paper_id, started_at, submitted_at, status, flagged)
            VALUES (:id, :examId, :candidateId, :paperId, :startedAt, :submittedAt, :status, FALSE)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("examId", examId)
            .setParameter("candidateId", UUID.randomUUID())
            .setParameter("paperId", UUID.randomUUID())
            .setParameter("startedAt", OffsetDateTime.ofInstant(startedAt, ZoneOffset.UTC))
            .setParameter("submittedAt", submittedAt == null ? null : OffsetDateTime.ofInstant(submittedAt, ZoneOffset.UTC))
            .setParameter("status", status)
            .executeUpdate();
    }

    /**
     * Điều kiện tiên quyết của mọi phép cắt ngày bên dưới. {@code timestamptz AT TIME ZONE 'zone'}
     * đổi sang giờ địa phương của zone — đúng thứ ta cần; nhưng nếu cột là {@code timestamp} TRẦN
     * thì cùng cú pháp đó làm điều NGƯỢC LẠI (coi giá trị đang ở zone rồi quy về UTC). Ghim kiểu cột
     * ở đây để nếu ai đổi nó, test đỏ kèm lý do thay vì kết quả lệch âm thầm.
     */
    @Test
    void submittedAtIsTimestampWithTimeZone() {
        var dataType = em.createNativeQuery("""
            SELECT data_type FROM information_schema.columns
            WHERE table_name = 'exam_sessions' AND column_name = 'submitted_at'
            """)
            .getSingleResult();

        assertThat(dataType).isEqualTo("timestamp with time zone");
    }

    @Test
    void countsLiveSessionsByStatusAndDistinctExams() {
        var examA = UUID.randomUUID();
        var examB = UUID.randomUUID();
        var now = Instant.parse("2026-08-15T03:00:00Z");

        insertSession(examA, "IN_PROGRESS", now, null);
        insertSession(examA, "IN_PROGRESS", now, null);
        insertSession(examB, "IN_PROGRESS", now, null);
        insertSession(examA, "SUBMITTED", now, now);
        insertSession(examB, "GRADING", now, now);
        // Nhiễu: đã kết thúc, không được rơi vào ô nào ở trên.
        insertSession(examA, "GRADED", now, now);
        insertSession(examA, "GRADING_FAILED", now, now);
        insertSession(examB, "EXPIRED", now, null);
        insertSession(examB, "INTERRUPTED", now, null);

        var live = repository.countLiveSessions();

        assertThat(live.sessionsInProgress()).isEqualTo(3L);
        // Ba phiên đang thi nhưng chỉ thuộc HAI kỳ thi.
        assertThat(live.examsInProgress()).isEqualTo(2L);
        assertThat(live.gradingQueueDepth()).isEqualTo(2L);
    }

    @Test
    void liveCountsAreZeroOnEmptyTable() {
        var live = repository.countLiveSessions();

        assertThat(live.sessionsInProgress()).isZero();
        assertThat(live.examsInProgress()).isZero();
        assertThat(live.gradingQueueDepth()).isZero();
    }

    /**
     * Phép kiểm QUAN TRỌNG NHẤT của file này. Việt Nam là UTC+7, nên mọi phiên nộp sau 17:00 UTC
     * thuộc về ngày HÔM SAU theo lịch VN. Cắt theo UTC sẽ đẩy chúng lùi một ngày — tức các ca thi
     * buổi sáng sớm rơi nhầm cột trên biểu đồ.
     */
    @Test
    void bucketsByVietnamCalendarDayNotUtcDay() {
        var exam = UUID.randomUUID();
        var started = Instant.parse("2026-08-10T01:00:00Z");

        // 23:00 giờ VN ngày 10/08 -> vẫn là ngày 10.
        insertSession(exam, "GRADED", started, Instant.parse("2026-08-10T16:00:00Z"));
        // Đúng 00:00 giờ VN ngày 11/08 -> đã sang ngày 11, dù UTC vẫn là ngày 10.
        insertSession(exam, "GRADED", started, Instant.parse("2026-08-10T17:00:00Z"));
        // 06:30 giờ VN ngày 11/08 -> ngày 11.
        insertSession(exam, "GRADED", started, Instant.parse("2026-08-10T23:30:00Z"));

        var buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).extracting(bucket -> bucket.day())
            .containsExactly(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11));
        assertThat(buckets).extracting(bucket -> bucket.graded())
            .containsExactly(1L, 2L);
    }

    @Test
    void separatesGradedFromFailedAndIgnoresOtherStatuses() {
        var exam = UUID.randomUUID();
        var started = Instant.parse("2026-08-05T01:00:00Z");
        var submitted = Instant.parse("2026-08-05T04:00:00Z");

        insertSession(exam, "GRADED", started, submitted);
        insertSession(exam, "GRADED", started, submitted);
        insertSession(exam, "GRADING_FAILED", started, submitted);
        // Chưa chấm xong: không thuộc mẫu số của tỷ lệ thành công.
        insertSession(exam, "SUBMITTED", started, submitted);
        insertSession(exam, "GRADING", started, submitted);
        insertSession(exam, "EXPIRED", started, submitted);

        var buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).day()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(buckets.get(0).graded()).isEqualTo(2L);
        assertThat(buckets.get(0).failed()).isEqualTo(1L);
    }

    /** {@code from} BAO GỒM, {@code to} KHÔNG bao gồm — nếu không, hai dải liền nhau đếm trùng. */
    @Test
    void windowIsHalfOpen() {
        var exam = UUID.randomUUID();
        var started = Instant.parse("2026-07-01T00:00:00Z");

        insertSession(exam, "GRADED", started, WINDOW_FROM);
        insertSession(exam, "GRADED", started, WINDOW_TO);
        insertSession(exam, "GRADED", started, WINDOW_FROM.minusMillis(1));

        var buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        var total = buckets.stream().mapToLong(bucket -> bucket.graded()).sum();
        assertThat(total).isEqualTo(1L);
    }

    @Test
    void returnsNoBucketsWhenNothingWasGradedInWindow() {
        insertSession(UUID.randomUUID(), "GRADED", Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"));

        var buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).isEmpty();
    }
}
