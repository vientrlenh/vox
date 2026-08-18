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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository.StudentScheduleConflict;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamScheduleJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Câu truy vấn phát hiện học sinh bị xếp hai ca thi trùng giờ.
 *
 * <p>Chạy trên Postgres thật vì JPQL trả {@code List<Object[]>} với join không mapping —
 * compiler không kiểm được gì, và biên nửa mở {@code [start, end)} là chỗ rất dễ sai.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class ExamCandidateConflictQueryTests extends ContainerTestConfig {

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
        // Ca của kỳ thi khác chạy 09:00–11:00, chồng một giờ lên khung đang xét.
        var busyScheduleId = seedSchedule(otherExamId, "PUBLISHED",
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
        // Ca kết thúc ĐÚNG lúc khung đang xét bắt đầu, và ca bắt đầu ĐÚNG lúc khung kết thúc.
        // Biên nửa mở [start, end) nên cả hai đều không phải trùng giờ.
        var before = seedSchedule(otherExamId, "PUBLISHED",
            windowStart.minus(2, ChronoUnit.HOURS), windowStart);
        seedCandidate(otherExamId, studentId, before, "ASSIGNED");

        var laterExamId = seedExam("Kỳ thi cuối kỳ");
        var after = seedSchedule(laterExamId, "PUBLISHED", windowEnd, windowEnd.plus(2, ChronoUnit.HOURS));
        seedCandidate(laterExamId, studentId, after, "ASSIGNED");

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_ignore_exempted_and_cancelled_candidates() {
        var exemptedSchedule = seedSchedule(otherExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherExamId, studentId, exemptedSchedule, "EXEMPTED");

        var cancelledExamId = seedExam("Kỳ thi đã rút");
        var cancelledSchedule = seedSchedule(cancelledExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(cancelledExamId, studentId, cancelledSchedule, "CANCELLED");

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_ignore_schedules_that_are_not_draft_or_published() {
        for (var status : List.of("COMPLETED", "MOVED", "CANCELLED", "DELETED")) {
            var examId = seedExam("Kỳ thi " + status);
            var scheduleId = seedSchedule(examId, status, windowStart, windowEnd);
            seedCandidate(examId, studentId, scheduleId, "ASSIGNED");
        }

        assertThat(conflicts()).isEmpty();
    }

    @Test
    void should_still_report_blocked_candidates() {
        // Bị chặn vào phòng vẫn là người thuộc ca đó — chỗ ngồi vẫn bị chiếm.
        var busyScheduleId = seedSchedule(otherExamId, "DRAFT", windowStart, windowEnd);
        var candidate = seedCandidateEntity(otherExamId, studentId, busyScheduleId, "ASSIGNED");
        candidate.setBlockedAt(now);
        em.flush();
        em.clear();

        assertThat(conflicts()).singleElement()
            .satisfies(conflict -> assertThat(conflict.scheduleId()).isEqualTo(busyScheduleId));
    }

    @Test
    void should_honour_the_excluded_schedule_id() {
        var busyScheduleId = seedSchedule(otherExamId, "PUBLISHED", windowStart, windowEnd);
        seedCandidate(otherExamId, studentId, busyScheduleId, "ASSIGNED");

        assertThat(examCandidateRepository.findConflictsForStudents(
            List.of(studentId), windowStart, windowEnd, busyScheduleId)).isEmpty();
    }

    @Test
    void should_only_report_the_students_asked_about() {
        var busyScheduleId = seedSchedule(otherExamId, "PUBLISHED", windowStart, windowEnd);
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
