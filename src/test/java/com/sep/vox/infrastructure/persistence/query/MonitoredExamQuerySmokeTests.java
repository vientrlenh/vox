package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.repository.MonitoredExamQueryRepository;
import com.sep.vox.config.ContainerTestConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Câu đọc của màn giám sát, chạy trên Postgres thật.
 *
 * <p>Dựng bằng {@code em.createQuery} nên nó chỉ được phân tích lúc GỌI: sai ở đây lọt qua compile,
 * lọt qua cả context load, và chỉ nổ khi giám thị bấm vào màn hình. Đúng chuyện đã xảy ra --
 * {@code (:leadUntil IS NULL OR ...)} dịch sang SQL hợp lệ nhưng Postgres từ chối vì không suy ra
 * được kiểu của tham số thời gian đứng trơ trong {@code IS NULL}.
 *
 * <p>Bốn tổ hợp dưới đây là bốn CÂU khác nhau, không phải bốn cách gọi một câu: mỗi tham số nullable
 * ghép hoặc bỏ một mệnh đề, nên chỉ chạy một tổ hợp là để ba câu còn lại không ai đọc.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class MonitoredExamQuerySmokeTests extends ContainerTestConfig {

    private static final Duration LEAD = Duration.ofMinutes(30);

    @Autowired
    private MonitoredExamQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    private UUID schoolId;
    private UUID teacherId;
    private UUID examId;
    private Instant now;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        examId = UUID.randomUUID();
        now = Instant.parse("2026-07-29T02:00:00Z");
    }

    @Test
    void should_list_a_running_exam_for_the_proctor_assigned_to_it() {
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)));

        var found = repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD));

        assertThat(found).singleElement().satisfies(exam -> {
            assertThat(exam.examId()).isEqualTo(examId);
            assertThat(exam.code()).isEqualTo("SMOKE-" + examId);
            // SUM(CASE ...) chỉ chạy khi có dòng: tổ hợp rỗng không chứng minh được nó ánh xạ về long.
            assertThat(exam.liveScheduleCount()).isEqualTo(1L);
            assertThat(exam.windowStart()).isEqualTo(now.minus(Duration.ofMinutes(10)));
        });
    }

    /** Ca chưa tới giờ vẫn phải hiện -- giám thị vào phòng trước khi học viên kết nối. */
    @Test
    void should_list_an_upcoming_exam_but_report_no_running_schedule() {
        seedSchedule(now.plus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)));

        var found = repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD));

        assertThat(found).singleElement()
            .satisfies(exam -> assertThat(exam.liveScheduleCount()).isZero());
    }

    /** Ngoài cửa sổ thì im -- nếu không thì "đang diễn ra" chỉ còn là cái nhãn. */
    @Test
    void should_ignore_a_schedule_that_starts_after_the_lead_window() {
        seedSchedule(now.plus(Duration.ofHours(3)), now.plus(Duration.ofHours(4)));

        assertThat(repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD))).isEmpty();
    }

    /**
     * Đường đọc một kỳ thi: {@code leadUntil} null nên BỎ lọc thời gian.
     *
     * <p>Ca đã kết thúc mà vẫn trả về chính là điều kiện để đầu trang xem lại có tên kỳ thi.
     */
    @Test
    void should_read_one_finished_exam_for_its_proctor_when_the_window_is_dropped() {
        seedSchedule(now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(3)));

        var found = repository.findMonitorableByTeacher(teacherId, examId, now, null);

        assertThat(found).singleElement()
            .satisfies(exam -> assertThat(exam.liveScheduleCount()).isZero());
    }

    @Test
    void should_not_read_an_exam_the_teacher_proctors_no_schedule_of() {
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)));

        assertThat(repository.findMonitorableByTeacher(UUID.randomUUID(), examId, now, null)).isEmpty();
    }

    @Test
    void should_list_a_running_exam_for_the_school_without_any_proctor_assignment() {
        seedExam();
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)), false);

        var found = repository.findMonitorableBySchool(schoolId, null, now, now.plus(LEAD));

        assertThat(found).singleElement()
            .satisfies(exam -> assertThat(exam.liveScheduleCount()).isEqualTo(1L));
    }

    @Test
    void should_read_one_exam_for_the_school_when_the_window_is_dropped() {
        seedExam();
        seedSchedule(now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(3)), false);

        assertThat(repository.findMonitorableBySchool(schoolId, examId, now, null)).hasSize(1);
    }

    @Test
    void should_not_read_an_exam_of_another_school() {
        seedExam();
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)), false);

        assertThat(repository.findMonitorableBySchool(UUID.randomUUID(), null, now, now.plus(LEAD)))
            .isEmpty();
    }

    private void seedSchedule(Instant start, Instant end) {
        seedExam();
        seedSchedule(start, end, true);
    }

    // Chèn thẳng bằng SQL: entity ở đây có constructor ~25 tham số, và test này chỉ cần đúng những
    // cột mà câu truy vấn đọc. Baseline không khai foreign key nào nên id trường/giáo viên tự do.
    private void seedExam() {
        em.createNativeQuery("""
            INSERT INTO exams (id, code, name, kind, status, language_id, school_id, requires_otp,
                               created_at, updated_at)
            VALUES (:id, :code, 'Kỳ thi khói', 'CENTRALIZED', 'IN_PROGRESS', :languageId, :schoolId,
                    true, :now, :now)
            """)
            .setParameter("id", examId)
            .setParameter("code", "SMOKE-" + examId)
            .setParameter("languageId", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .executeUpdate();
    }

    private void seedSchedule(Instant start, Instant end, boolean assignTeacher) {
        var scheduleId = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO exam_schedules (id, exam_id, start_date, end_date, status, created_at, updated_at)
            VALUES (:id, :examId, :start, :end, 'PUBLISHED', :now, :now)
            """)
            .setParameter("id", scheduleId)
            .setParameter("examId", examId)
            .setParameter("start", start)
            .setParameter("end", end)
            .setParameter("now", now)
            .executeUpdate();
        if (assignTeacher) {
            em.createNativeQuery("""
                INSERT INTO exam_schedule_proctors (id, schedule_id, teacher_id)
                VALUES (:id, :scheduleId, :teacherId)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("scheduleId", scheduleId)
                .setParameter("teacherId", teacherId)
                .executeUpdate();
        }
    }
}
