package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Danh tính học sinh trên màn chấm: <strong>hiện với bài kiểm tra TRÊN LỚP, ẩn với kỳ
 * thi TẬP TRUNG.</strong>
 *
 * <p>Ca đối chứng {@code should_keep_student_anonymous_for_centralized_exam} là phần
 * quan trọng nhất của lớp này. Không có nó thì một lần sửa query sau này làm rò danh
 * tính của kỳ thi tập trung — tức phá vỡ chấm mù, thứ bảo đảm công bằng — mà không
 * test nào đỏ.
 *
 * <p>Phải chạy trên Postgres thật vì đây là JPQL dựng bằng {@code em.createQuery}: sai
 * tên trường chỉ nổ lúc GỌI chứ không lúc biên dịch.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class GradingStudentIdentityQueryTests {

    @Autowired
    private ExamGradingQueryRepository examGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private UUID classTestExamId;
    private UUID centralizedExamId;

    @BeforeEach
    void seed() {
        UUID schoolId = UUID.randomUUID();
        UUID studentId = persist(new UserJpaEntity(null, "hs-" + UUID.randomUUID() + "@vox.test", "hash",
            null, "Trần Quang Thiên", null, LocalDate.of(2008, 1, 1), null, null, "ACTIVE",
            now, now, null, null)).getId();
        UUID schoolClassId = persist(new SchoolClassJpaEntity(schoolId, UUID.randomUUID(), UUID.randomUUID(),
            "10A1-" + UUID.randomUUID().toString().substring(0, 8), "10A1", null, "ACTIVE",
            now, now, teacherId, teacherId)).getId();
        persist(new SchoolClassUserJpaEntity(studentId, schoolClassId, true, now, null, teacherId));

        classTestExamId = seedExam(schoolId, "CLASS_TEST", "Kiểm tra 15 phút", studentId);
        centralizedExamId = seedExam(schoolId, "CENTRALIZED", "Kỳ thi cuối kỳ", studentId);

        em.flush();
        em.clear();
    }

    @Test
    void should_expose_student_and_class_for_class_test_task() {
        GradingTaskInfo task = onlyTaskOf(classTestExamId);

        assertThat(task.studentName()).isEqualTo("Trần Quang Thiên");
        assertThat(task.className()).isEqualTo("10A1");
    }

    /** Control case: blind grading of a centralized exam must not leak. */
    @Test
    void should_keep_student_anonymous_for_centralized_exam() {
        GradingTaskInfo task = onlyTaskOf(centralizedExamId);

        assertThat(task.studentName()).isNull();
        assertThat(task.className()).isNull();
        // The submission code is still the only thing that identifies a submission on the anonymous
        // screen.
        assertThat(task.resultCode()).isNotBlank();
    }

    /**
     * A queue that does NOT filter by exam kind mixes both -- each row must follow the rules of its
     * own exam, not the rules of the first row on the page. Every use case passes the kind, so this
     * mixing only remains at the repository layer, but the per-row rule must still be correct.
     */
    @Test
    void should_decide_per_row_when_the_queue_mixes_both_kinds() {
        PageResult<GradingTaskInfo> page = examGradingQueryRepository.findTasksByTeacherId(teacherId, null, null, null, 1, 20);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content())
            .filteredOn(task -> "Kiểm tra 15 phút".equals(task.examName()))
            .singleElement()
            .satisfies(task -> assertThat(task.studentName()).isEqualTo("Trần Quang Thiên"));
        assertThat(page.content())
            .filteredOn(task -> "Kỳ thi cuối kỳ".equals(task.examName()))
            .singleElement()
            .satisfies(task -> assertThat(task.studentName()).isNull());
    }

    @Test
    void should_expose_student_and_class_on_the_grading_detail_of_a_class_test() {
        GradingTaskDetailInfo detail = examGradingQueryRepository
            .findTaskDetail(onlyTaskOf(classTestExamId).assignmentId(), teacherId).orElseThrow();

        assertThat(detail.studentName()).isEqualTo("Trần Quang Thiên");
        assertThat(detail.className()).isEqualTo("10A1");
    }

    @Test
    void should_keep_the_grading_detail_anonymous_for_a_centralized_exam() {
        GradingTaskDetailInfo detail = examGradingQueryRepository
            .findTaskDetail(onlyTaskOf(centralizedExamId).assignmentId(), teacherId).orElseThrow();

        assertThat(detail.studentName()).isNull();
        assertThat(detail.className()).isNull();
    }

    private GradingTaskInfo onlyTaskOf(UUID examId) {
        PageResult<GradingTaskInfo> page = examGradingQueryRepository.findTasksByTeacherIdAndExamId(
            teacherId, examId, null, null, null, 1, 20);
        assertThat(page.content()).hasSize(1);
        return page.content().get(0);
    }

    private UUID seedExam(UUID schoolId, String kind, String name, UUID studentId) {
        UUID paperId = UUID.randomUUID();
        UUID examId = persist(new ExamJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), kind, "STUDENT_DEVICE", "CLOSED",
            1, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
        UUID candidateId = persist(new ExamCandidateJpaEntity(null, examId, studentId, paperId, null,
            "COMPLETED", now, now, null, null, null)).getId();
        UUID sessionId = persist(new ExamSessionJpaEntity(null, examId, candidateId, paperId, now, now,
            "GRADED", false, null)).getId();
        UUID candidateResultId = persist(new ExamCandidateResultJpaEntity(null, examId, candidateId, sessionId,
            UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
            new BigDecimal("6.50"), "PENDING_REVIEW", null, null, now, now, null, null)).getId();
        persist(new ExamGradingAssignmentJpaEntity(null, candidateResultId, teacherId,
            "INITIAL", null, "ASSIGNED", null, new BigDecimal("6.50"), now, teacherId, null,
            null, null, null, candidateResultId));
        return examId;
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
