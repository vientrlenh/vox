package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository.StudentScheduleConflict;
import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentScheduleJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Câu truy vấn phát hiện học sinh bị xếp hai ca thi trùng giờ.
 *
 * <p>Chạy trên Postgres thật vì JPQL trả {@code List<Object[]>} với join không mapping —
 * compiler không kiểm được gì, và biên nửa mở {@code [start, end)} là chỗ rất dễ sai.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class AssessmentCandidateConflictQueryTests {

    @Autowired
    private AssessmentCandidateRepository assessmentCandidateRepository;

    @PersistenceContext
    private EntityManager em;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final UUID schoolId = UUID.randomUUID();

    /** Khung giờ đang được cân nhắc xếp học sinh vào: 08:00–10:00. */
    private Instant windowStart;
    private Instant windowEnd;

    private UUID studentId;
    private UUID otherAssessmentId;

    @BeforeEach
    void seed() {
        windowStart = now.plus(1, ChronoUnit.DAYS);
        windowEnd = windowStart.plus(2, ChronoUnit.HOURS);
        studentId = UUID.randomUUID();
        otherAssessmentId = seedAssessment("Kỳ thi giữa kỳ");
    }

    @Test
    void should_find_a_student_booked_in_an_overlapping_schedule_of_another_assessment() {
        // A session of another assessment runs 09:00-11:00, overlapping the window under test by one
        // hour.
        UUID busyScheduleId = seedSchedule(otherAssessmentId, "PUBLISHED",
            windowStart.plus(1, ChronoUnit.HOURS), windowEnd.plus(1, ChronoUnit.HOURS));
        seedCandidate(otherAssessmentId, studentId, busyScheduleId, "ASSIGNED");

        assertThat(conflicts()).singleElement().satisfies(conflict -> {
            assertThat(conflict.studentId()).isEqualTo(studentId);
            assertThat(conflict.scheduleId()).isEqualTo(busyScheduleId);
            assertThat(conflict.startDate()).isEqualTo(windowStart.plus(1, ChronoUnit.HOURS));
        });
    }

    @Test
    void should_not_treat_back_to_back_schedules_as_a_conflict() {
        // A session that ends EXACTLY when the window under test starts, and a session that starts
        // EXACTLY when the window ends. The boundary is half-open [start, end), so neither is an
        // overlap.
        UUID before = seedSchedule(otherAssessmentId, "PUBLISHED",
            windowStart.minus(2, ChronoUnit.HOURS), windowStart);
        seedCandidate(otherAssessmentId, studentId, before, "ASSIGNED");

        UUID laterAssessmentId = seedAssessment("Kỳ thi cuối kỳ");
        UUID after = seedSchedule(laterAssessmentId, "PUBLISHED", windowEnd, windowEnd.plus(2, ChronoUnit.HOURS));
        seedCandidate(laterAssessmentId, studentId, after, "ASSIGNED");

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_ignore_exempted_and_cancelled_candidates() {
        UUID exemptedSchedule = seedSchedule(otherAssessmentId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherAssessmentId, studentId, exemptedSchedule, "EXEMPTED");

        UUID cancelledAssessmentId = seedAssessment("Kỳ thi đã rút");
        UUID cancelledSchedule = seedSchedule(cancelledAssessmentId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(cancelledAssessmentId, studentId, cancelledSchedule, "CANCELLED");

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_ignore_schedules_that_are_not_draft_or_published() {
        for (String status : List.of("COMPLETED", "MOVED", "CANCELLED", "DELETED")) {
            UUID assessmentId = seedAssessment("Kỳ thi " + status);
            UUID scheduleId = seedSchedule(assessmentId, status, windowStart, windowEnd);
            seedCandidate(assessmentId, studentId, scheduleId, "ASSIGNED");
        }

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_still_report_blocked_candidates() {
        // Being blocked from the room, the person still belongs to that session -- the seat is
        // still taken.
        UUID busyScheduleId = seedSchedule(otherAssessmentId, "DRAFT", windowStart, windowEnd);
        AssessmentCandidateJpaEntity candidate = seedCandidateEntity(otherAssessmentId, studentId, busyScheduleId, "ASSIGNED");
        candidate.setBlockedAt(now);
        em.flush();
        em.clear();

        assertThat(conflicts()).singleElement()
            .satisfies(conflict -> assertThat(conflict.scheduleId()).isEqualTo(busyScheduleId));
    }

    @Test
    void should_honour_the_excluded_schedule_id() {
        UUID busyScheduleId = seedSchedule(otherAssessmentId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherAssessmentId, studentId, busyScheduleId, "ASSIGNED");

        assertThat(assessmentCandidateRepository.findConflictsForStudents(
            List.of(studentId), windowStart, windowEnd, busyScheduleId)).isEmpty();
    }

    @Test
    void should_only_report_the_students_asked_about() {
        UUID busyScheduleId = seedSchedule(otherAssessmentId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherAssessmentId, UUID.randomUUID(), busyScheduleId, "ASSIGNED");

        assertThat(conflicts()).isEmpty();
    }

    private List<StudentScheduleConflict> conflicts() {
        em.flush();
        em.clear();
        return assessmentCandidateRepository.findConflictsForStudents(
            List.of(studentId), windowStart, windowEnd, null);
    }

    private UUID seedAssessment(String name) {
        return persist(new AssessmentJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), "CLASS_TEST", "STUDENT_DEVICE", "SCHEDULED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
    }

    private UUID seedSchedule(UUID assessmentId, String status, Instant start, Instant end) {
        return persist(new AssessmentScheduleJpaEntity(null, assessmentId, null, start, end, status,
            null, now, now, null, null)).getId();
    }

    private UUID seedCandidate(UUID assessmentId, UUID studentId, UUID scheduleId, String status) {
        return seedCandidateEntity(assessmentId, studentId, scheduleId, status).getId();
    }

    private AssessmentCandidateJpaEntity seedCandidateEntity(
            UUID assessmentId, UUID studentId, UUID scheduleId, String status) {
        return persist(new AssessmentCandidateJpaEntity(null, assessmentId, studentId, null, scheduleId,
            status, now, now, null, null, null));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
