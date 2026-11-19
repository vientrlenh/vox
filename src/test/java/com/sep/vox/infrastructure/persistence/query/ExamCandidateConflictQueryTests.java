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

import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamScheduleJpaEntity;
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
class ExamCandidateConflictQueryTests {

    @Autowired
    private ExamCandidateRepository examCandidateRepository;

    @PersistenceContext
    private EntityManager em;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final UUID schoolId = UUID.randomUUID();

    /** Khung giờ đang được cân nhắc xếp học sinh vào: 08:00–10:00. */
    private Instant windowStart;
    private Instant windowEnd;

    private UUID studentId;
    private UUID otherExamId;

    @BeforeEach
    void seed() {
        windowStart = now.plus(1, ChronoUnit.DAYS);
        windowEnd = windowStart.plus(2, ChronoUnit.HOURS);
        studentId = UUID.randomUUID();
        otherExamId = seedExam("Kỳ thi giữa kỳ");
    }

    @Test
    void should_find_a_student_booked_in_an_overlapping_schedule_of_another_exam() {
        // A session of another exam runs 09:00-11:00, overlapping the window under test by one
        // hour.
        UUID busyScheduleId = seedSchedule(otherExamId, "PUBLISHED",
            windowStart.plus(1, ChronoUnit.HOURS), windowEnd.plus(1, ChronoUnit.HOURS));
        seedCandidate(otherExamId, studentId, busyScheduleId, "ASSIGNED");

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
        UUID before = seedSchedule(otherExamId, "PUBLISHED",
            windowStart.minus(2, ChronoUnit.HOURS), windowStart);
        seedCandidate(otherExamId, studentId, before, "ASSIGNED");

        UUID laterExamId = seedExam("Kỳ thi cuối kỳ");
        UUID after = seedSchedule(laterExamId, "PUBLISHED", windowEnd, windowEnd.plus(2, ChronoUnit.HOURS));
        seedCandidate(laterExamId, studentId, after, "ASSIGNED");

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_ignore_exempted_and_cancelled_candidates() {
        UUID exemptedSchedule = seedSchedule(otherExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherExamId, studentId, exemptedSchedule, "EXEMPTED");

        UUID cancelledExamId = seedExam("Kỳ thi đã rút");
        UUID cancelledSchedule = seedSchedule(cancelledExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(cancelledExamId, studentId, cancelledSchedule, "CANCELLED");

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_ignore_schedules_that_are_not_draft_or_published() {
        for (String status : List.of("COMPLETED", "MOVED", "CANCELLED", "DELETED")) {
            UUID examId = seedExam("Kỳ thi " + status);
            UUID scheduleId = seedSchedule(examId, status, windowStart, windowEnd);
            seedCandidate(examId, studentId, scheduleId, "ASSIGNED");
        }

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_still_report_blocked_candidates() {
        // Being blocked from the room, the person still belongs to that session -- the seat is
        // still taken.
        UUID busyScheduleId = seedSchedule(otherExamId, "DRAFT", windowStart, windowEnd);
        ExamCandidateJpaEntity candidate = seedCandidateEntity(otherExamId, studentId, busyScheduleId, "ASSIGNED");
        candidate.setBlockedAt(now);
        em.flush();
        em.clear();

        assertThat(conflicts()).singleElement()
            .satisfies(conflict -> assertThat(conflict.scheduleId()).isEqualTo(busyScheduleId));
    }

    @Test
    void should_honour_the_excluded_schedule_id() {
        UUID busyScheduleId = seedSchedule(otherExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherExamId, studentId, busyScheduleId, "ASSIGNED");

        assertThat(examCandidateRepository.findConflictsForStudents(
            List.of(studentId), windowStart, windowEnd, busyScheduleId)).isEmpty();
    }

    @Test
    void should_only_report_the_students_asked_about() {
        UUID busyScheduleId = seedSchedule(otherExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherExamId, UUID.randomUUID(), busyScheduleId, "ASSIGNED");

        assertThat(conflicts()).isEmpty();
    }

    private List<StudentScheduleConflict> conflicts() {
        em.flush();
        em.clear();
        return examCandidateRepository.findConflictsForStudents(
            List.of(studentId), windowStart, windowEnd, null);
    }

    private UUID seedExam(String name) {
        return persist(new ExamJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), "CLASS_TEST", "STUDENT_DEVICE", "SCHEDULED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
    }

    private UUID seedSchedule(UUID examId, String status, Instant start, Instant end) {
        return persist(new ExamScheduleJpaEntity(null, examId, null, start, end, status,
            null, now, now, null, null)).getId();
    }

    private UUID seedCandidate(UUID examId, UUID studentId, UUID scheduleId, String status) {
        return seedCandidateEntity(examId, studentId, scheduleId, status).getId();
    }

    private ExamCandidateJpaEntity seedCandidateEntity(
            UUID examId, UUID studentId, UUID scheduleId, String status) {
        return persist(new ExamCandidateJpaEntity(null, examId, studentId, null, scheduleId,
            status, now, now, null, null, null));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
