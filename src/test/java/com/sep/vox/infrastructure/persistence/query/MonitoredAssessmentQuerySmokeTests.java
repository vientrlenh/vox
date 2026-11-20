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

    /** Ngoài cửa sổ thì im -- nếu không thì "đang diễn ra" chỉ còn là cái nhãn. */
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
     * Ca đã publish nhưng KỲ THI còn nháp thì màn giám sát phải im.
     *
     * <p>Không phải tổ hợp bịa: luồng chuẩn bắt publish từng ca TRƯỚC rồi mới đẩy kỳ thi sang
     * SCHEDULED, nên đây đúng là trạng thái mọi kỳ thi đều đi qua. Lọc theo {@code sch.status} thôi
     * thì cả hai vai đều thấy kỳ thi nhà trường chưa chốt.
     */
    @Test
    void should_hide_an_assessment_still_in_draft() {
        seedAssessment("DRAFT");
        seedSchedule(now.minus(Duration.ofMinutes(10)), now.plus(Duration.ofHours(1)), true);

        assertThat(repository.findMonitorableByTeacher(teacherId, null, now, now.plus(LEAD))).isEmpty();
        assertThat(repository.findMonitorableBySchool(schoolId, null, now, now.plus(LEAD))).isEmpty();
        // Cả đường đọc một kỳ thi (bỏ cửa sổ thời gian) cũng phải câm -- đó là đường mở thẳng bằng URL.
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

    // Chèn thẳng bằng SQL: entity ở đây có constructor ~25 tham số, và test này chỉ cần đúng những
    // cột mà câu truy vấn đọc. Baseline không khai foreign key nào nên id trường/giáo viên tự do.
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
