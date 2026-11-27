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
 * Runs REAL SQL on a real Postgres. The use case's unit test mocks the repository, so it proves the part that
 * builds the date series but proves NOTHING about the SQL itself: {@code COUNT(*) FILTER},
 * {@code AT TIME ZONE}, and the way Hibernate returns the {@code date} column and the {@code COUNT} value to Java are
 * all only exposed with a real DB -- exactly the kind of trap that caused a ClassCastException in
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

    private void insertSession(UUID assessmentId, String status, Instant startedAt, Instant submittedAt) {
        em.createNativeQuery("""
            INSERT INTO assessment_sessions (id, assessment_id, candidate_id, paper_id, started_at, submitted_at, status, flagged)
            VALUES (:id, :assessmentId, :candidateId, :paperId, :startedAt, :submittedAt, :status, FALSE)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("assessmentId", assessmentId)
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
            WHERE table_name = 'assessment_sessions' AND column_name = 'submitted_at'
            """)
            .getSingleResult();

        assertThat(dataType).isEqualTo("timestamp with time zone");
    }

    @Test
    void countsLiveSessionsByStatusAndDistinctAssessments() {
        UUID assessmentA = UUID.randomUUID();
        UUID assessmentB = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T03:00:00Z");

        insertSession(assessmentA, "IN_PROGRESS", now, null);
        insertSession(assessmentA, "IN_PROGRESS", now, null);
        insertSession(assessmentB, "IN_PROGRESS", now, null);
        insertSession(assessmentA, "SUBMITTED", now, now);
        insertSession(assessmentB, "GRADING", now, now);
        // Noise: already finished, must not fall into any cell above.
        insertSession(assessmentA, "GRADED", now, now);
        insertSession(assessmentA, "GRADING_FAILED", now, now);
        insertSession(assessmentB, "EXPIRED", now, null);
        insertSession(assessmentB, "INTERRUPTED", now, null);

        LiveSessionCountsDto live = repository.countLiveSessions();

        assertThat(live.sessionsInProgress()).isEqualTo(3L);
        // Three sessions in progress but they belong to only TWO assessments.
        assertThat(live.assessmentsInProgress()).isEqualTo(2L);
        assertThat(live.gradingQueueDepth()).isEqualTo(2L);
    }

    @Test
    void liveCountsAreZeroOnEmptyTable() {
        LiveSessionCountsDto live = repository.countLiveSessions();

        assertThat(live.sessionsInProgress()).isZero();
        assertThat(live.assessmentsInProgress()).isZero();
        assertThat(live.gradingQueueDepth()).isZero();
    }

    /**
     * The MOST IMPORTANT check of this file. Vietnam is UTC+7, so every session submitted after
     * 17:00 UTC belongs to the NEXT day on the Vietnam calendar. Cutting by UTC would push them
     * back a day -- i.e. early-morning assessment sessions land in the wrong column of the chart.
     */
    @Test
    void bucketsByVietnamCalendarDayNotUtcDay() {
        UUID assessment = UUID.randomUUID();
        Instant started = Instant.parse("2026-08-10T01:00:00Z");

        // 23:00 Vietnam time on 10/08 -> still the 10th.
        insertSession(assessment, "GRADED", started, Instant.parse("2026-08-10T16:00:00Z"));
        // Exactly 00:00 Vietnam time on 11/08 -> already the 11th, even though in UTC it is still
        // the 10th.
        insertSession(assessment, "GRADED", started, Instant.parse("2026-08-10T17:00:00Z"));
        // 06:30 Vietnam time on 11/08 -> the 11th.
        insertSession(assessment, "GRADED", started, Instant.parse("2026-08-10T23:30:00Z"));

        List<GradingOutcomeBucketDto> buckets = repository.findGradingOutcomeByDay(WINDOW_FROM, WINDOW_TO, ZoneConstant.BUSINESS_ZONE);

        assertThat(buckets).extracting(bucket -> bucket.day())
            .containsExactly(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11));
        assertThat(buckets).extracting(bucket -> bucket.graded())
            .containsExactly(1L, 2L);
    }

    @Test
    void separatesGradedFromFailedAndIgnoresOtherStatuses() {
        UUID assessment = UUID.randomUUID();
        Instant started = Instant.parse("2026-08-05T01:00:00Z");
        Instant submitted = Instant.parse("2026-08-05T04:00:00Z");

        insertSession(assessment, "GRADED", started, submitted);
        insertSession(assessment, "GRADED", started, submitted);
        insertSession(assessment, "GRADING_FAILED", started, submitted);
        // Grading not finished: not part of the denominator of the success rate.
        insertSession(assessment, "SUBMITTED", started, submitted);
        insertSession(assessment, "GRADING", started, submitted);
        insertSession(assessment, "EXPIRED", started, submitted);

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
        UUID assessment = UUID.randomUUID();
        Instant started = Instant.parse("2026-07-01T00:00:00Z");

        insertSession(assessment, "GRADED", started, WINDOW_FROM);
        insertSession(assessment, "GRADED", started, WINDOW_TO);
        insertSession(assessment, "GRADED", started, WINDOW_FROM.minusMillis(1));

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
