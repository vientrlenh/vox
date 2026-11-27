package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.MonitoredAssessmentSummary;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.application.query.repository.MonitoredAssessmentQueryRepository;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The read statements of the monitoring screen, running on a real Postgres.
 *
 * <p>Built with {@code em.createQuery} so it is only parsed at CALL time: a mistake here slips through compile,
 * slips through context load, and only blows up when a proctor opens the screen. Exactly what happened --
 * {@code (:leadUntil IS NULL OR ...)} translates to valid SQL but Postgres rejects it because it cannot infer
 * the type of a time parameter standing alone in {@code IS NULL}.
 *
 * <p>The four combinations below are four different STATEMENTS, not four ways of calling one statement: each nullable parameter
 * adds or drops a clause, so running only one combination leaves the other three statements unread by anyone.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class MonitoredAssessmentQuerySmokeTests {

    private static final Duration LEAD = Duration.ofMinutes(30);

    @Autowired
    private MonitoredAssessmentQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    private UUID schoolId;
    private UUID teacherId;
    private UUID assessmentId;
    private Instant now;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        assessmentId = UUID.randomUUID();
        now = Instant.parse("2026-07-29T02:00:00Z");
    }

    @Test
    void should_list_a_running_assessment_for_the_proctor_assigned_to_it() {
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)));

        List<MonitoredAssessmentSummary> found = repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD));

        assertThat(found).singleElement().satisfies(assessment -> {
            assertThat(assessment.assessmentId()).isEqualTo(assessmentId);
            assertThat(assessment.code()).isEqualTo("SMOKE-" + assessmentId);
            assertThat(assessment.kind()).isEqualTo(AssessmentKind.CENTRALIZED);
            assertThat(assessment.status()).isEqualTo(AssessmentStatus.IN_PROGRESS);
            // SUM(CASE ...) only runs when there are rows: an empty combination cannot prove that
            // it maps to long.
            assertThat(assessment.liveScheduleCount()).isEqualTo(1L);
            assertThat(assessment.windowStart()).isEqualTo(now.minus(Duration.ofMinutes(10)));
        });
    }

    /**
     * A session that has not reached its time must still show -- proctors enter the room before
     * students connect.
     */
    @Test
    void should_list_an_upcoming_assessment_but_report_no_running_schedule() {
        seedSchedule(now.plus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)));

        List<MonitoredAssessmentSummary> found = repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD));

        assertThat(found).singleElement()
            .satisfies(assessment -> assertThat(assessment.liveScheduleCount()).isZero());
    }

    /** Outside the window it stays silent -- otherwise "in progress" is just a label. */
    @Test
    void should_ignore_a_schedule_that_starts_after_the_lead_window() {
        seedSchedule(now.plus(Duration.ofHours(3)), now.plus(Duration.ofHours(4)));

        assertThat(repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD))).isEmpty();
    }

    /**
     * The single-assessment read path: {@code leadUntil} is null so the time filter is DROPPED.
     *
     * <p>A session that has ended and is still returned is exactly the condition for the review
     * page to carry the assessment name at the top.
     */
    @Test
    void should_read_one_finished_assessment_for_its_proctor_when_the_window_is_dropped() {
        seedSchedule(now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(3)));

        List<MonitoredAssessmentSummary> found = repository.findMonitorableByTeacher(teacherId, assessmentId, now, null);

        assertThat(found).singleElement()
            .satisfies(assessment -> assertThat(assessment.liveScheduleCount()).isZero());
    }

    @Test
    void should_not_read_an_assessment_the_teacher_proctors_no_schedule_of() {
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)));

        assertThat(repository.findMonitorableByTeacher(UUID.randomUUID(), assessmentId, now, null)).isEmpty();
    }

    @Test
    void should_list_a_running_assessment_for_the_school_without_any_proctor_assignment() {
        seedAssessment();
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)), false);

        List<MonitoredAssessmentSummary> found = repository.findMonitorableBySchool(schoolId, null, now, now.plus(LEAD));

        assertThat(found).singleElement()
            .satisfies(assessment -> assertThat(assessment.liveScheduleCount()).isEqualTo(1L));
    }

    @Test
    void should_read_one_assessment_for_the_school_when_the_window_is_dropped() {
        seedAssessment();
        seedSchedule(now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(3)), false);

        assertThat(repository.findMonitorableBySchool(schoolId, assessmentId, now, null)).hasSize(1);
    }

    /**
     * A schedule that is published while its ASSESSMENT is still a draft must keep the monitoring screen silent.
     *
     * <p>Not a made-up combination: the standard flow publishes each schedule FIRST and only then moves the assessment to
     * SCHEDULED, so this is exactly a state every assessment passes through. Filtering by {@code sch.status} alone
     * would let both roles see an assessment the school has not finalized.
     */
    @Test
    void should_hide_an_assessment_still_in_draft() {
        seedAssessment("DRAFT");
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)), true);

        assertThat(repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD))).isEmpty();
        assertThat(repository.findMonitorableBySchool(schoolId, null, now, now.plus(LEAD))).isEmpty();
        // The single-assessment read path (without the time window) must be silent too -- that is the path opened directly by URL.
        assertThat(repository.findMonitorableBySchool(schoolId, assessmentId, now, null)).isEmpty();
    }

    @Test
    void should_not_read_an_assessment_of_another_school() {
        seedAssessment();
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)), false);

        assertThat(repository.findMonitorableBySchool(UUID.randomUUID(), null, now, now.plus(LEAD)))
            .isEmpty();
    }

    private void seedSchedule(Instant start, Instant end) {
        seedAssessment();
        seedSchedule(start, end, true);
    }

    private void seedAssessment() {
        seedAssessment("IN_PROGRESS");
    }

    // Inserted directly with SQL: the entity here has a constructor with ~25 parameters, and this test only needs the
    // columns that the query reads. The baseline declares no foreign keys so the school/teacher ids are free.
    private void seedAssessment(String status) {
        em.createNativeQuery("""
            INSERT INTO assessments (id, code, name, kind, status, language_id, school_id, requires_otp,
                               created_at, updated_at)
            VALUES (:id, :code, 'Kỳ thi khói', 'CENTRALIZED', :status, :languageId, :schoolId,
                    true, :now, :now)
            """)
            .setParameter("id", assessmentId)
            .setParameter("code", "SMOKE-" + assessmentId)
            .setParameter("status", status)
            .setParameter("languageId", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .executeUpdate();
    }

    private void seedSchedule(Instant start, Instant end, boolean assignTeacher) {
        UUID scheduleId = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO assessment_schedules (id, assessment_id, start_date, end_date, status, created_at, updated_at)
            VALUES (:id, :assessmentId, :start, :end, 'PUBLISHED', :now, :now)
            """)
            .setParameter("id", scheduleId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("start", start)
            .setParameter("end", end)
            .setParameter("now", now)
            .executeUpdate();
        if (assignTeacher) {
            em.createNativeQuery("""
                INSERT INTO assessment_schedule_proctors (id, schedule_id, teacher_id)
                VALUES (:id, :scheduleId, :teacherId)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("scheduleId", scheduleId)
                .setParameter("teacherId", teacherId)
                .executeUpdate();
        }
    }
}
