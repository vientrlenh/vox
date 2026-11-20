package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Tách kỳ thi TẬP TRUNG khỏi bài kiểm tra TRÊN LỚP ở mọi read model chấm bài.
 *
 * <p>Hai loại bài đi hai màn khác nhau — nhà trường điều phối kỳ thi tập trung, giáo viên
 * tạo bài tự chấm bài trên lớp. Trước khi có bộ lọc này, bảng điều phối liệt kê cả bài
 * trên lớp rồi bấm gán là bị chặn ở tầng dưới, còn hàng đợi của giáo viên thì trộn hai
 * loại vào một danh sách.
 *
 * <p>Phải chạy trên Postgres thật vì đây là JPQL dựng bằng {@code em.createQuery}: sai
 * tên trường chỉ nổ lúc GỌI chứ không lúc biên dịch.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class GradingExamKindFilterQueryTests extends ContainerTestConfig {

    @Autowired
    private ExamGradingQueryRepository examGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private UUID schoolId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();
        var studentId = persist(new UserJpaEntity(null, "hs-" + UUID.randomUUID() + "@vox.test", "hash",
            null, "Trần Quang Thiên", null, LocalDate.of(2008, 1, 1), null, null, "ACTIVE",
            now, now, null, null)).getId();

        seedExam("CLASS_TEST", "Kiểm tra 15 phút", studentId);
        seedExam("CENTRALIZED", "Kỳ thi cuối kỳ", studentId);

        em.flush();
        em.clear();
    }

    @Test
    void should_list_only_centralized_rows_on_the_coordination_board() {
        var page = examGradingQueryRepository.searchAssignments(filter("CENTRALIZED"), 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(row -> assertThat(row.examName()).isEqualTo("Kỳ thi cuối kỳ"));
        // Tổng phải đi cùng nội dung, nếu không FE dựng ra trang rỗng không bấm được.
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_list_only_class_test_rows_when_asked_for_class_tests() {
        var page = examGradingQueryRepository.searchAssignments(filter("CLASS_TEST"), 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(row -> assertThat(row.examName()).isEqualTo("Kiểm tra 15 phút"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    /** Bộ lọc bỏ trống vẫn trả cả hai — mặc định nằm ở use case, không ẩn trong SQL. */
    @Test
    void should_list_both_kinds_when_no_kind_is_given() {
        assertThat(examGradingQueryRepository.searchAssignments(filter(null), 1, 20).content())
            .hasSize(2);
    }

    @Test
    void should_count_only_the_asked_kind_in_stats() {
        var centralized = examGradingQueryRepository.stats(schoolId, null, null, "CENTRALIZED");
        var classTest = examGradingQueryRepository.stats(schoolId, null, null, "CLASS_TEST");

        assertThat(centralized.total()).isEqualTo(1);
        assertThat(centralized.assigned()).isEqualTo(1);
        assertThat(classTest.total()).isEqualTo(1);
        assertThat(classTest.assigned()).isEqualTo(1);
        assertThat(centralized.teacherProgress()).hasSize(1);
    }

    @Test
    void should_keep_class_test_assignments_out_of_the_centralized_teacher_queue() {
        var page = examGradingQueryRepository
            .findTasksByTeacherId(teacherId, "CENTRALIZED", null, null, 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(task -> assertThat(task.examName()).isEqualTo("Kỳ thi cuối kỳ"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_keep_centralized_assignments_out_of_the_class_test_queue() {
        var page = examGradingQueryRepository
            .findTasksByTeacherId(teacherId, "CLASS_TEST", null, null, 1, 20);

        assertThat(page.content()).singleElement()
            .satisfies(task -> assertThat(task.examName()).isEqualTo("Kiểm tra 15 phút"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    /**
     * Auto-assign là hành động của nhà trường; bài trên lớp lọt vào đây là mỗi lần chạy
     * lại gắp lên một mớ bài rồi bị chặn ở tầng dưới.
     */
    @Test
    void should_never_offer_class_test_results_to_auto_assign() {
        // Bài đã có phân công đang mở nên tập trả về rỗng; điều cần chốt là bài trên lớp
        // không xuất hiện kể cả khi trạng thái của nó hợp lệ với vòng chấm.
        var assignable = examGradingQueryRepository.findAssignableResultIds(
            schoolId, null, null, List.of("PENDING_REVIEW", "RELEASED"));

        assertThat(assignable).isEmpty();
    }

    private GradingAssignmentFilter filter(String examKind) {
        return new GradingAssignmentFilter(
            schoolId, null, null, null, null, null, null, false, false, null, null, examKind);
    }

    private void seedExam(String kind, String name, UUID studentId) {
        var paperId = UUID.randomUUID();
        var examId = persist(new ExamJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), kind, "STUDENT_DEVICE", "CLOSED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
        var candidateId = persist(new ExamCandidateJpaEntity(null, examId, studentId, paperId, null,
            "COMPLETED", now, now, null, null, null)).getId();
        var sessionId = persist(new ExamSessionJpaEntity(null, examId, candidateId, paperId, now, now,
            "GRADED", false, null)).getId();
        var candidateResultId = persist(new ExamCandidateResultJpaEntity(null, examId, candidateId, sessionId,
            UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
            new BigDecimal("6.50"), "PENDING_REVIEW", null, null, now, now, null, null)).getId();
        persist(new ExamGradingAssignmentJpaEntity(null, candidateResultId, teacherId,
            "INITIAL", null, "ASSIGNED", null, new BigDecimal("6.50"), now, teacherId, null,
            null, null, null, candidateResultId));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
