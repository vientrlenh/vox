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

import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;
import com.sep.vox.config.ContainerTestConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Câu đọc của màn điểm danh giám thị, chạy trên Postgres thật.
 *
 * <p>Dựng bằng {@code em.createQuery} nên chỉ được phân tích lúc GỌI: sai ở đây lọt qua compile, lọt
 * qua cả context load, và chỉ nổ khi giám thị bấm vào màn hình -- cùng lý do
 * {@link MonitoredExamQuerySmokeTests} phải tồn tại.
 *
 * <p>Trọng tâm là bộ lọc trạng thái, vì đây là chỗ đã sai một lần: màn điểm danh từng hiện cả kỳ thi
 * còn DRAFT. {@code exams.status} và {@code exam_schedules.status} là hai máy trạng thái ĐỘC LẬP --
 * luồng chuẩn bắt người xếp lịch publish từng ca TRƯỚC rồi mới đẩy kỳ thi sang SCHEDULED -- nên một
 * ca PUBLISHED nằm dưới kỳ thi DRAFT là dữ liệu hợp lệ, không phải rác. Lọc theo ca thôi thì không
 * đủ.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class JpaProctorScheduleQuerySmokeTests extends ContainerTestConfig {

    @Autowired
    private ProctorScheduleQueryRepository repository;

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
    void should_list_a_published_schedule_of_a_scheduled_exam() {
        seed("SCHEDULED", "PUBLISHED");

        assertThat(repository.findByTeacherId(teacherId)).singleElement().satisfies(schedule -> {
            assertThat(schedule.examId()).isEqualTo(examId);
            assertThat(schedule.examName()).isEqualTo("Kỳ thi khói");
            assertThat(schedule.status()).isEqualTo("PUBLISHED");
            // LEFT JOIN phòng thi: ca chưa xếp phòng vẫn phải ra dòng, không bị join nuốt mất.
            assertThat(schedule.roomName()).isNull();
            assertThat(schedule.startDate()).isEqualTo(now.minus(Duration.ofMinutes(10)));
        });
        assertThat(repository.findBySchoolId(schoolId)).hasSize(1);
    }

    /**
     * Ca đã publish nhưng KỲ THI còn nháp: nhà trường chưa chốt thì màn điểm danh chưa được hiện.
     * Đây là ca tái hiện đúng lỗi đã báo.
     */
    @Test
    void should_hide_a_schedule_of_an_exam_still_in_draft() {
        seed("DRAFT", "PUBLISHED");

        assertThat(repository.findByTeacherId(teacherId)).isEmpty();
        assertThat(repository.findBySchoolId(schoolId)).isEmpty();
    }

    /** Ca chưa publish là ca chưa xếp xong (chưa đủ giám thị, chưa gán đề) -- chưa có gì để điểm danh. */
    @Test
    void should_hide_a_schedule_still_in_draft() {
        seed("SCHEDULED", "DRAFT");

        assertThat(repository.findByTeacherId(teacherId)).isEmpty();
        assertThat(repository.findBySchoolId(schoolId)).isEmpty();
    }

    /** Chỉ ẩn DRAFT: kỳ thi bị huỷ vẫn hiện kèm trạng thái để giám thị biết mà không tới phòng. */
    @Test
    void should_still_list_a_cancelled_exam() {
        seed("CANCELLED", "CANCELLED");

        assertThat(repository.findByTeacherId(teacherId)).singleElement()
            .satisfies(schedule -> assertThat(schedule.status()).isEqualTo("CANCELLED"));
        assertThat(repository.findBySchoolId(schoolId)).hasSize(1);
    }

    /** Ca đã dời hoặc đã xoá mềm không còn là ca thật -- hành vi cũ, giữ khỏi bị gỡ nhầm. */
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

    // Chèn thẳng bằng SQL: entity ở đây có constructor rất dài, và test này chỉ cần đúng những cột mà
    // câu truy vấn đọc. Baseline không khai foreign key nào nên id trường/giáo viên tự do.
    private void seed(String examStatus, String scheduleStatus) {
        var scheduleId = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO exams (id, code, name, kind, status, language_id, school_id, requires_otp,
                               created_at, updated_at)
            VALUES (:id, :code, 'Kỳ thi khói', 'CENTRALIZED', :status, :languageId, :schoolId,
                    true, :now, :now)
            ON CONFLICT (id) DO NOTHING
            """)
            .setParameter("id", examId)
            .setParameter("code", "SMOKE-" + examId)
            .setParameter("status", examStatus)
            .setParameter("languageId", UUID.randomUUID())
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .executeUpdate();
        em.createNativeQuery("""
            INSERT INTO exam_schedules (id, exam_id, start_date, end_date, status, created_at, updated_at)
            VALUES (:id, :examId, :start, :end, :status, :now, :now)
            """)
            .setParameter("id", scheduleId)
            .setParameter("examId", examId)
            .setParameter("start", now.minus(Duration.ofMinutes(10)))
            .setParameter("end", now.plus(Duration.ofHours(1)))
            .setParameter("status", scheduleStatus)
            .setParameter("now", now)
            .executeUpdate();
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
