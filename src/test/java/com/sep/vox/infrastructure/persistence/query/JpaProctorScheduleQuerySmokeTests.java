package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The read for the proctor attendance screen, running on real Postgres.
 *
 * <p>Built with {@code em.createQuery} so it is only parsed at CALL time: a mistake here passes compilation, passes
 * even the context load, and only blows up when a proctor clicks into the screen -- the same reason
 * {@link MonitoredAssessmentQuerySmokeTests} has to exist.
 *
 * <p>The focus is the status filter, because this is the place that was wrong once: the attendance screen once showed even assessments
 * still in DRAFT. {@code assessments.status} and {@code assessment_schedules.status} are two INDEPENDENT state machines --
 * the standard flow requires the scheduler to publish each session FIRST and only then push the assessment to SCHEDULED -- so a
 * PUBLISHED session under a DRAFT assessment is valid data, not junk. Filtering by session alone is not
 * enough.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class JpaProctorScheduleQuerySmokeTests {

    @Autowired
    private ProctorScheduleQueryRepository repository;

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
    void should_list_a_published_schedule_of_a_scheduled_assessment() {
        seed("SCHEDULED", "PUBLISHED");

        assertThat(repository.findByTeacherId(teacherId)).singleElement().satisfies(schedule -> {
            assertThat(schedule.assessmentId()).isEqualTo(assessmentId);
            assertThat(schedule.assessmentName()).isEqualTo("Kỳ thi khói");
            assertThat(schedule.status()).isEqualTo(AssessmentScheduleStatus.PUBLISHED);
            // LEFT JOIN on the exam room: a schedule with no room assigned must still produce a row, not be swallowed by the join.
            assertThat(schedule.roomName()).isNull();
            assertThat(schedule.startDate()).isEqualTo(now.minus(Duration.ofMinutes(10)));
        });
        assertThat(repository.findBySchoolId(schoolId)).hasSize(1);
    }

    /**
     * A schedule that is published while its ASSESSMENT is still a draft: the school has not finalized, so the attendance screen must not show yet.
     * This reproduces exactly the reported bug.
     */
    @Test
    void should_hide_a_schedule_of_an_assessment_still_in_draft() {
        seed("DRAFT", "PUBLISHED");

        assertThat(repository.findByTeacherId(teacherId)).isEmpty();
        assertThat(repository.findBySchoolId(schoolId)).isEmpty();
    }

    /** An unpublished schedule is one not fully arranged (not enough proctors, no paper assigned) -- nothing to take attendance of yet. */
    @Test
    void should_hide_a_schedule_still_in_draft() {
        seed("SCHEDULED", "DRAFT");

        assertThat(repository.findByTeacherId(teacherId)).isEmpty();
        assertThat(repository.findBySchoolId(schoolId)).isEmpty();
    }

    /** Only DRAFT is hidden: a cancelled assessment still shows with its status so the proctor knows not to go to the room. */
    @Test
    void should_still_list_a_cancelled_assessment() {
        seed("CANCELLED", "CANCELLED");

        assertThat(repository.findByTeacherId(teacherId)).singleElement()
            .satisfies(schedule -> assertThat(schedule.status()).isEqualTo(AssessmentScheduleStatus.CANCELLED));
        assertThat(repository.findBySchoolId(schoolId)).hasSize(1);
    }

    /** A schedule that was moved or soft-deleted is no longer a real schedule -- old behavior, kept so it is not removed by mistake. */
    @Test
    void should_hide_moved_and_deleted_schedules() {
        seed("SCHEDULED", "MOVED");
        seed("SCHEDULED", "DELETED");

        assertThat(repository.findByTeacherId(teacherId)).isEmpty();
        assertThat(repository.findBySchoolId(schoolId)).isEmpty();
    }

    @Test
    void should_not_read_schedules_of_another_teacher() {
        seed("SCHEDULED", "PUBLISHED");

        assertThat(repository.findByTeacherId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_not_read_schedules_of_another_school() {
        seed("SCHEDULED", "PUBLISHED");

        assertThat(repository.findBySchoolId(UUID.randomUUID())).isEmpty();
    }

    // Inserted directly with SQL: the entity here has a very long constructor, and this test only needs the columns that
    // the query reads. The baseline declares no foreign keys so the school/teacher ids are free.
    private void seed(String assessmentStatus, String scheduleStatus) {
        UUID scheduleId = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO assessments (id, code, name, kind, status, language_id, school_id, requires_otp,
                               created_at, updated_at)
            VALUES (:id, :code, 'Kỳ thi khói', 'CENTRALIZED', :status, :languageId, :schoolId,
                    true, :now, :now)
            ON CONFLICT (id) DO NOTHING
            """)
            .setParameter("id", assessmentId)
            .setParameter("code", "SMOKE-" + assessmentId)
            .setParameter("status", assessmentStatus)
            .setParameter("languageId", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .executeUpdate();
        em.createNativeQuery("""
            INSERT INTO assessment_schedules (id, assessment_id, start_date, end_date, status, created_at, updated_at)
            VALUES (:id, :assessmentId, :start, :end, :status, :now, :now)
            """)
            .setParameter("id", scheduleId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("start", now.minus(Duration.ofMinutes(10)))
            .setParameter("end", now.plus(Duration.ofHours(1)))
            .setParameter("status", scheduleStatus)
            .setParameter("now", now)
            .executeUpdate();
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
