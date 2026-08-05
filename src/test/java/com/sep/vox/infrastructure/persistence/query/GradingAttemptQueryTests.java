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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
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
 * Số lượt thi trên màn chấm.
 *
 * <p>Một học sinh thi lại sinh ra nhiều {@code exam_sessions}, mỗi phiên một
 * {@code exam_candidate_results} riêng — tức nhiều DÒNG cùng tên trên màn chấm. Không có
 * {@code attemptNo}/{@code attemptCount} thì người chấm nhìn hai dòng y hệt nhau và không
 * biết mình đang chấm lượt nào.
 *
 * <p>DB không có cột số lượt: thứ tự suy từ {@code startedAt}. Ca "lượt 2" là ca quan
 * trọng nhất ở đây — nó chốt rằng bài đang xét được đối chiếu đúng với phiên của nó chứ
 * không phải cứ lấy phiên đầu tiên.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class GradingAttemptQueryTests extends ContainerTestConfig {

    @Autowired
    private ExamGradingQueryRepository examGradingQueryRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID teacherId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private static final String RETAKING_STUDENT = "Trần Quang Thiên";
    private static final String SINGLE_ATTEMPT_STUDENT = "Lê Văn Việt";

    private UUID schoolId;
    private UUID examId;

    @BeforeEach
    void seed() {
        schoolId = UUID.randomUUID();
        var retakingStudentId = persist(user(RETAKING_STUDENT)).getId();
        var singleAttemptStudentId = persist(user(SINGLE_ATTEMPT_STUDENT)).getId();

        examId = seedExam("Kiểm tra 15 phút");

        var retakingCandidateId = seedCandidate(examId, retakingStudentId);
        // Hai lượt CÁCH NHAU về thời gian: thứ tự lượt suy từ startedAt, trùng mốc thì
        // không còn thứ tự để suy.
        seedAttempt(examId, retakingCandidateId, now.minus(2, ChronoUnit.HOURS));
        seedAttempt(examId, retakingCandidateId, now.minus(1, ChronoUnit.HOURS));

        var singleCandidateId = seedCandidate(examId, singleAttemptStudentId);
        seedAttempt(examId, singleCandidateId, now.minus(2, ChronoUnit.HOURS));

        em.flush();
        em.clear();
    }

    @Test
    void should_number_both_attempts_of_the_same_student_on_the_board() {
        var rows = boardRowsOf(RETAKING_STUDENT);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.attemptNo()).containsExactlyInAnyOrder(1, 2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.attemptCount()).isEqualTo(2);
            assertThat(row.sessionId()).isNotNull();
        });
        // Mỗi dòng phải trỏ về đúng phiên của mình — trùng sessionId nghĩa là đang lấy
        // nhầm một phiên cho cả hai lượt.
        assertThat(rows.get(0).sessionId()).isNotEqualTo(rows.get(1).sessionId());
    }

    @Test
    void should_report_a_single_attempt_when_the_student_took_the_exam_once() {
        assertThat(boardRowsOf(SINGLE_ATTEMPT_STUDENT)).singleElement().satisfies(row -> {
            assertThat(row.attemptNo()).isEqualTo(1);
            assertThat(row.attemptCount()).isEqualTo(1);
        });
    }

    private List<GradingAssignmentRowInfo> boardRowsOf(String studentName) {
        return examGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId, examId, null, null, null, null, null, false, false, null, null,
            "CLASS_TEST"), 0, 20).content().stream()
            .filter(row -> studentName.equals(row.studentName()))
            .toList();
    }

    @Test
    void should_number_attempts_in_the_teacher_queue() {
        var tasks = examGradingQueryRepository
            .findTasksByTeacherIdAndExamId(teacherId, examId, null, null, null, 0, 20).content();

        assertThat(tasks).hasSize(3);
        assertThat(tasks).filteredOn(task -> task.attemptCount() == 2)
            .extracting(task -> task.attemptNo())
            .containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void should_number_the_attempt_on_the_grading_detail() {
        var secondAttempt = examGradingQueryRepository
            .findTasksByTeacherIdAndExamId(teacherId, examId, null, null, null, 0, 20).content().stream()
            .filter(task -> task.attemptNo() == 2)
            .findFirst()
            .orElseThrow();

        var detail = examGradingQueryRepository
            .findTaskDetail(secondAttempt.assignmentId(), teacherId).orElseThrow();

        assertThat(detail.attemptNo()).isEqualTo(2);
        assertThat(detail.attemptCount()).isEqualTo(2);
        assertThat(detail.sessionId()).isEqualTo(secondAttempt.sessionId());
    }

    private UserJpaEntity user(String fullName) {
        return new UserJpaEntity(null, "hs-" + UUID.randomUUID() + "@vox.test", "hash",
            null, fullName, null, LocalDate.of(2008, 1, 1), null, null, "ACTIVE",
            now, now, null, null);
    }

    private UUID seedExam(String name) {
        return persist(new ExamJpaEntity(null, null, null, "EX-" + UUID.randomUUID(), name,
            null, schoolId, UUID.randomUUID(), "CLASS_TEST", "STUDENT_DEVICE", "CLOSED",
            2, 900, "HIGHEST", null, null, null, null, UUID.randomUUID(), false,
            now, now, null, null)).getId();
    }

    private UUID seedCandidate(UUID examId, UUID studentId) {
        return persist(new ExamCandidateJpaEntity(null, examId, studentId, UUID.randomUUID(), null,
            "COMPLETED", now, now, null, null, null)).getId();
    }

    private void seedAttempt(UUID examId, UUID candidateId, Instant startedAt) {
        var sessionId = persist(new ExamSessionJpaEntity(null, examId, candidateId, UUID.randomUUID(),
            startedAt, startedAt, "GRADED", false, null)).getId();
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
