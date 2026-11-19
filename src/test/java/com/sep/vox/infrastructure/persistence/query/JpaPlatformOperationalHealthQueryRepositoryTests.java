package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;
import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT trên Postgres thật. Test unit của use case mock repository nên chứng minh được phần
 * dựng chuỗi ngày, nhưng KHÔNG chứng minh được gì về bản thân câu SQL: {@code COUNT(*) FILTER},
 * {@code AT TIME ZONE}, và cách Hibernate trả cột {@code date} lẫn giá trị {@code COUNT} về Java đều
 * chỉ lộ ra khi có DB thật — đúng loại bẫy đã gây ClassCastException ở
 * {@code JpaTokenUsageTimeseriesQueryRepository}.
 */
@RepositoryTest
@Import(JpaPlatformOperationalHealthQueryRepository.class)
class JpaPlatformOperationalHealthQueryRepositoryTests {

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
     * The precondition of every day-cut below. {@code timestamptz AT TIME ZONE 'zone'} converts to
     * the zone's local time -- exactly what we need; but if the column is a BARE {@code timestamp},
     * the same syntax does the OPPOSITE (it treats the value as already being in the zone and
     * converts it to UTC). The column type is pinned here so that if someone changes it, the test
     * turns red with a reason instead of the results drifting silently.
     */
    @Test
    void submittedAtIsTimestampWithTimeZone() {
        Object dataType = em.createNativeQuery("""
            SELECT data_type FROM information_schema.columns
            WHERE table_name = 'exam_sessions' AND column_name = 'submitted_at'
            """)
            .getSingleResult();

        assertThat(dataType).isEqualTo("timestamp with time zone");
    }

    @Test
    void countsLiveSessionsByStatusAndDistinctExams() {
        UUID examA = UUID.randomUUID();
        UUID examB = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T03:00:00Z");

        insertSession(examA, "IN_PROGRESS", now, null);
        insertSession(examA, "IN_PROGRESS", now, null);
        insertSession(examB, "IN_PROGRESS", now, null);
        insertSession(examA, "SUBMITTED", now, now);
        insertSession(examB, "GRADING", now, now);
        // Noise: already finished, must not fall into any cell above.
        insertSession(examA, "GRADED", now, now);
        insertSession(examA, "GRADING_FAILED", now, now);
        insertSession(examB, "EXPIRED", now, null);
        insertSession(examB, "INTERRUPTED", now, null);

        LiveSessionCountsDto live = repository.countLiveSessions();

        assertThat(live.sessionsInProgress()).isEqualTo(3L);
        // Three sessions in progress but they belong to only TWO exams.
        assertThat(live.examsInProgress()).isEqualTo(2L);
        assertThat(live.gradingQueueDepth()).isEqualTo(2L);
    }

    @Test
    void liveCountsAreZeroOnEmptyTable() {
        LiveSessionCountsDto live = repository.countLiveSessions();

        assertThat(live.sessionsInProgress()).isZero();
        assertThat(live.examsInProgress()).isZero();
        assertThat(live.gradingQueueDepth()).isZero();
    }

    /**
     * The MOST IMPORTANT check of this file. Vietnam is UTC+7, so every session submitted after
     * 17:00 UTC belongs to the NEXT day on the Vietnam calendar. Cutting by UTC would push them
     * back a day -- i.e. early-morning exam sessions land in the wrong column of the chart.
     */
    @Test
    void bucketsByVietnamCalendarDayNotUtcDay() {
        UUID exam = UUID.randomUUID();
        Instant started = Instant.parse("2026-08-10T01:00:00Z");

        // 23:00 Vietnam time on 10/08 -> still the 10th.
        insertSession(exam, "GRADED", started, Instant.parse("2026-08-10T16:00:00Z"));
        // Exactly 00:00 Vietnam time on 11/08 -> already the 11th, even though in UTC it is still
        // the 10th.
        insertSession(exam, "GRADED", started, Instant.parse("2026-08-10T17:00:00Z"));
        // 06:30 Vietnam time on 11/08 -> the 11th.
        insertSession(exam, "GRADED", started, Instant.parse("2026-08-10T23:30:00Z"));

        List<GradingOutcomeBucketDto> buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).extracting(bucket -> bucket.day())
            .containsExactly(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11));
        assertThat(buckets).extracting(bucket -> bucket.graded())
            .containsExactly(1L, 2L);
    }

    @Test
    void separatesGradedFromFailedAndIgnoresOtherStatuses() {
        UUID exam = UUID.randomUUID();
        Instant started = Instant.parse("2026-08-05T01:00:00Z");
        Instant submitted = Instant.parse("2026-08-05T04:00:00Z");

        insertSession(exam, "GRADED", started, submitted);
        insertSession(exam, "GRADED", started, submitted);
        insertSession(exam, "GRADING_FAILED", started, submitted);
        // Grading not finished: not part of the denominator of the success rate.
        insertSession(exam, "SUBMITTED", started, submitted);
        insertSession(exam, "GRADING", started, submitted);
        insertSession(exam, "EXPIRED", started, submitted);

        List<GradingOutcomeBucketDto> buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).day()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(buckets.get(0).graded()).isEqualTo(2L);
        assertThat(buckets.get(0).failed()).isEqualTo(1L);
    }

    /**
     * {@code from} is INCLUSIVE, {@code to} is NOT inclusive -- otherwise two adjacent ranges would
     * count twice.
     */
    @Test
    void windowIsHalfOpen() {
        UUID exam = UUID.randomUUID();
        Instant started = Instant.parse("2026-07-01T00:00:00Z");

        insertSession(exam, "GRADED", started, WINDOW_FROM);
        insertSession(exam, "GRADED", started, WINDOW_TO);
        insertSession(exam, "GRADED", started, WINDOW_FROM.minusMillis(1));

        List<GradingOutcomeBucketDto> buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        long total = buckets.stream().mapToLong(bucket -> bucket.graded()).sum();
        assertThat(total).isEqualTo(1L);
    }

    @Test
    void returnsNoBucketsWhenNothingWasGradedInWindow() {
        insertSession(UUID.randomUUID(), "GRADED", Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"));

        List<GradingOutcomeBucketDto> buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).isEmpty();
    }
}
