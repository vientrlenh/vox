package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingExamOptionInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Bộ lọc kỳ thi của hàng đợi giáo viên: danh sách đổ vào dropdown, và việc lọc theo kỳ
 * thi đã chọn.
 *
 * <p>Phạm vi của dropdown suy TỪ tập phân công chứ không từ bảng kỳ thi — giáo viên chấm
 * kỳ thi tập trung không phải thành viên kỳ thi nên mọi lối đọc exams thông thường đều
 * đóng với họ. Test này chốt hai điều: dropdown chỉ ra kỳ thi của chính người gọi, và
 * {@code examId} chỉ THU HẸP chứ không mở thêm cửa nào.
 *
 * <p>Chạy trên Postgres thật vì đây là JPQL dựng bằng {@code em.createQuery}: sai tên
 * trường chỉ nổ lúc GỌI chứ không lúc biên dịch.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class TeacherGradingExamPickerQueryTests {

    @Autowired
    private ExamGradingQueryRepository examGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID otherTeacherId = UUID.randomUUID();
    private final Instant now = Instant.now();
    private final Instant longAgo = Instant.now().minus(5, ChronoUnit.DAYS);

    private UUID schoolId;
    private UUID midtermExamId;
    private UUID finalExamId;
    private UUID otherTeacherExamId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();

        // Kỳ thi giữa kỳ: 2 bài còn phải chấm + 1 bài đã chấm xong, giao gần đây nhất.
        midtermExamId = seedExam("CENTRALIZED", "Kỳ thi giữa kỳ");
        seedAssignment(midtermExamId, teacherId, "ASSIGNED", now);
        seedAssignment(midtermExamId, teacherId, "ASSIGNED", now);
        seedAssignment(midtermExamId, teacherId, "COMPLETED", now);

        // Kỳ thi cuối kỳ: chấm xong sạch, giao từ lâu.
        finalExamId = seedExam("CENTRALIZED", "Kỳ thi cuối kỳ");
        seedAssignment(finalExamId, teacherId, "COMPLETED", longAgo);

        // Bài kiểm tra trên lớp có màn riêng — không được lọt vào dropdown này.
        seedAssignment(seedExam("CLASS_TEST", "Kiểm tra 15 phút"), teacherId, "ASSIGNED", now);

        // Kỳ thi của giáo viên khác, cùng trường.
        otherTeacherExamId = seedExam("CENTRALIZED", "Kỳ thi của người khác");
        seedAssignment(otherTeacherExamId, otherTeacherId, "ASSIGNED", now);

        em.flush();
        em.clear();
    }

    @Test
    void should_list_only_the_exams_the_teacher_has_assignments_in() {
        assertThat(pickerFor(teacherId))
            .extracting(option -> option.name())
            .containsExactlyInAnyOrder("Kỳ thi giữa kỳ", "Kỳ thi cuối kỳ");
    }

    /**
     * Chấm xong bài cuối cùng mà kỳ thi biến mất khỏi dropdown ngay dưới tay là lỗi:
     * giáo viên vẫn phải xem lại được những bài mình đã chấm.
     */
    @Test
    void should_still_list_an_exam_whose_assignments_are_all_completed() {
        assertThat(pickerFor(teacherId))
            .filteredOn(option -> option.id().equals(finalExamId))
            .singleElement()
            .satisfies(option -> {
                assertThat(option.taskCount()).isEqualTo(1);
                assertThat(option.openTaskCount()).isZero();
            });
    }

    @Test
    void should_keep_class_test_exams_out_of_the_centralized_picker() {
        assertThat(pickerFor(teacherId))
            .extracting(option -> option.name())
            .doesNotContain("Kiểm tra 15 phút");
    }

    @Test
    void should_count_open_and_completed_tasks_per_exam() {
        assertThat(pickerFor(teacherId))
            .filteredOn(option -> option.id().equals(midtermExamId))
            .singleElement()
            .satisfies(option -> {
                assertThat(option.taskCount()).isEqualTo(3);
                assertThat(option.openTaskCount()).isEqualTo(2);
            });
    }

    /** Kỳ thi vừa được giao là kỳ thi đang chấm dở — thứ giáo viên tìm trước nhất. */
    @Test
    void should_order_exams_by_the_most_recent_assignment_first() {
        assertThat(pickerFor(teacherId))
            .extracting(option -> option.id())
            .containsExactly(midtermExamId, finalExamId);
    }

    @Test
    void should_return_nothing_for_a_teacher_with_no_assignments() {
        assertThat(pickerFor(UUID.randomUUID())).isEmpty();
    }

    @Test
    void should_filter_the_teacher_queue_by_exam_id() {
        PageResult<GradingTaskInfo> page = examGradingQueryRepository.findTasksByTeacherIdAndExamId(
            teacherId, midtermExamId, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.content()).hasSize(3)
            .allSatisfy(task -> assertThat(task.examName()).isEqualTo("Kỳ thi giữa kỳ"));
        assertThat(page.totalElements()).isEqualTo(3);
    }

    /**
     * {@code examId} is a filter, NOT a gate: the teacherId condition still stands next to it, so
     * guessing another person's exam id opens nothing.
     *
     * <p>It also pins {@code totalElements}: a filter that narrows the content without narrowing
     * the count makes the FE build empty pages that cannot be clicked.
     */
    @Test
    void should_not_leak_another_teachers_tasks_when_filtering_by_exam_id() {
        PageResult<GradingTaskInfo> page = examGradingQueryRepository.findTasksByTeacherIdAndExamId(
            teacherId, otherTeacherExamId, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    /**
     * Leaving examId empty = every centralized exam of the caller, as before the filter existed.
     */
    @Test
    void should_return_every_centralized_task_when_no_exam_id_is_given() {
        PageResult<GradingTaskInfo> page = examGradingQueryRepository.findTasksByTeacherIdAndExamId(
            teacherId, null, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(4);
    }

    private List<GradingExamOptionInfo> pickerFor(UUID who) {
        return examGradingQueryRepository.findExamsWithTasksByTeacherId(who, "CENTRALIZED");
    }

    private UUID seedStudent() {
        return persist(new UserJpaEntity(null, "hs-" + UUID.randomUUID() + "@vox.test", "hash",
            null, "Trần Quang Thiên", null, LocalDate.of(2008, 1, 1), null, null, "ACTIVE",
            now, now, null, null)).getId();
    }

    private UUID seedExam(String kind, String name) {
        return persist(new ExamJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), kind, "STUDENT_DEVICE", "CLOSED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
    }

    /**
     * Each assignment needs its own submission, and each submission needs its own STUDENT: a
     * student can take an exam only once ({@code uq_exam_candidates_exam_student}), and
     * {@code active_result_id} has a partial unique index, so a submission can have at most ONE
     * open assignment.
     */
    private void seedAssignment(UUID examId, UUID assignedTo, String status, Instant assignedAt) {
        UUID paperId = UUID.randomUUID();
        UUID studentId = seedStudent();
        UUID candidateId = persist(new ExamCandidateJpaEntity(null, examId, studentId, paperId, null,
            "COMPLETED", now, now, null, null, null)).getId();
        UUID sessionId = persist(new ExamSessionJpaEntity(null, examId, candidateId, paperId, now, now,
            "GRADED", false, null)).getId();
        UUID candidateResultId = persist(new ExamCandidateResultJpaEntity(null, examId, candidateId,
            sessionId, UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            null, new BigDecimal("6.50"), "PENDING_REVIEW", null, null, now, now, null, null)).getId();

        boolean completed = "COMPLETED".equals(status);
        persist(new ExamGradingAssignmentJpaEntity(null, candidateResultId, assignedTo,
            "INITIAL", null, status, completed ? "UPHELD" : null, new BigDecimal("6.50"),
            assignedAt, assignedTo, completed ? assignedAt : null, null, null, null,
            // A closed row has a null activeResultId -- that is how the partial index works.
            completed ? null : candidateResultId));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
