package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.config.ContainerTestConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT: giá trị của repository này nằm ở chỗ năm nhóm LOẠI TRỪ NHAU và phủ kín. Điều đó chỉ
 * kiểm được bằng cách dựng đủ năm tình huống trong cùng một trường rồi đếm — một test với repository
 * giả sẽ khẳng định đúng những gì nó tự dựng ra.
 *
 * <p>Cái bẫy lớn nhất mà bộ test này canh: một phiên đã chuyển sang chấm tay VẪN mang trạng thái
 * {@code GRADING_FAILED}. Đếm theo trạng thái phiên là xếp bài đã có người nhận vào nhóm "chưa ai xử
 * lý", và trên giao diện nó sẽ mời quản trị trường bấm chấm lại đè lên bài giáo viên đang chấm dở.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaSchoolWorkloadQueryRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaSchoolWorkloadQueryRepositoryTests extends ContainerTestConfig {

    private static final Instant NOW = Instant.parse("2026-09-01T03:00:00Z");
    private static final Instant SUBMITTED = Instant.parse("2026-08-27T02:14:00Z");

    @Autowired
    private SchoolWorkloadQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    /** Năm nhóm, mỗi nhóm một bài: tổng phải bằng 5 và không bài nào bị đếm hai lần. */
    @Test
    void shouldPartitionUnscoredWorkIntoFiveExclusiveBuckets() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");

        insertFailedSession(exam, SUBMITTED, 0);
        insertFailedSession(exam, SUBMITTED, 1);
        insertPendingReviewResult(exam, insertSession(exam, SUBMITTED, "GRADED"));
        insertAssignment(insertPendingReviewResult(exam, insertSession(exam, SUBMITTED, "GRADED")),
            NOW.minusSeconds(3600));
        insertAssignment(insertPendingReviewResult(exam, insertSession(exam, SUBMITTED, "GRADED")),
            NOW.plusSeconds(86400));

        var counts = repository.countUnscored(school, NOW);

        assertThat(counts.aiFailedRetryLeft()).isEqualTo(1);
        assertThat(counts.aiFailedNoRetryLeft()).isEqualTo(1);
        assertThat(counts.awaitingAssignment()).isEqualTo(1);
        assertThat(counts.assignedOverdue()).isEqualTo(1);
        assertThat(counts.assignedInProgress()).isEqualTo(1);
        assertThat(counts.total()).isEqualTo(5);
        assertThat(counts.examCount()).isEqualTo(1);
    }

    /**
     * Chuyển sang chấm tay KHÔNG đổi trạng thái phiên — cố ý, xem {@code HandOffGradingToHumanUseCase}.
     * Bài phải rời nhóm "AI hỏng, chưa ai xử lý" và sang hàng đợi người chấm.
     */
    @Test
    void shouldMoveAHandedOffPaperOutOfTheUntouchedAiBucket() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var sessionId = insertFailedSession(exam, SUBMITTED, 0);
        insertPendingReviewResult(exam, sessionId);

        var counts = repository.countUnscored(school, NOW);

        assertThat(counts.aiFailed()).isZero();
        assertThat(counts.awaitingAssignment()).isEqualTo(1);
        assertThat(counts.total()).isEqualTo(1);
    }

    /**
     * Định mức một lượt của trường quyết định bài còn cứu được bằng AI hay bắt buộc phải xếp giáo
     * viên. Hai con số này là thứ quản trị trường đọc TRƯỚC khi bấm công bố điểm.
     */
    @Test
    void shouldSplitFailedPapersByRemainingAiRetryAllowance() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(exam, SUBMITTED, 0);
        insertFailedSession(exam, SUBMITTED, 0);
        insertFailedSession(exam, SUBMITTED, 1);

        var counts = repository.countUnscored(school, NOW);

        assertThat(counts.aiFailedRetryLeft()).isEqualTo(2);
        assertThat(counts.aiFailedNoRetryLeft()).isEqualTo(1);
        assertThat(counts.aiFailed()).isEqualTo(3);
    }

    /**
     * Kỳ đã công bố điểm KHÔNG còn lối ra nào (cả hai use case đều từ chối ở trạng thái đó), nên bài
     * của nó không phải việc phải làm mà là thiệt hại đã xảy ra. Để chúng trong hàng đợi thì con số
     * có một mức sàn không bao giờ về 0.
     */
    @Test
    void shouldExcludePapersOfExamsWhoseResultsAreAlreadyPublished() {
        var school = insertSchool();
        var openExam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var publishedExam = insertExam(school, "Thi thử", "RESULTS_PUBLISHED");
        var cancelledExam = insertExam(school, "Kỳ đã hủy", "CANCELLED");
        insertFailedSession(openExam, SUBMITTED, 0);
        insertFailedSession(publishedExam, SUBMITTED, 0);
        insertFailedSession(cancelledExam, SUBMITTED, 0);

        assertThat(repository.countUnscored(school, NOW).total()).isEqualTo(1);
    }

    /** Bài kiểm tra trên lớp có màn điều phối riêng của giáo viên ra đề — gộp vào đây là lệch số. */
    @Test
    void shouldIgnoreClassTests() {
        var school = insertSchool();
        var classTest = insertExam(school, "KT 15 phút", "CLOSED", "CLASS_TEST");
        insertFailedSession(classTest, SUBMITTED, 0);

        assertThat(repository.countUnscored(school, NOW).total()).isZero();
    }

    @Test
    void shouldReportTheOldestWaitingPaper() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(exam, Instant.parse("2026-08-27T02:14:00Z"), 0);
        insertFailedSession(exam, Instant.parse("2026-08-20T01:00:00Z"), 0);

        assertThat(repository.countUnscored(school, NOW).oldestSubmittedAt())
            .isEqualTo(Instant.parse("2026-08-20T01:00:00Z"));
    }

    /** Hàng đợi sạch phải ra null, không phải 0: 0 ngày chờ nghĩa là "có bài, vừa nộp hôm nay". */
    @Test
    void shouldReturnNoOldestPaperWhenNothingIsWaiting() {
        var school = insertSchool();
        insertExam(school, "Giữa kỳ I", "CLOSED");

        var counts = repository.countUnscored(school, NOW);

        assertThat(counts.total()).isZero();
        assertThat(counts.oldestSubmittedAt()).isNull();
    }

    /** Chỉ kỳ CLOSED — đúng tập kỳ mà nút "Công bố điểm" đang bấm được. */
    @Test
    void shouldListOnlyClosedExamsThatStillHaveUnscoredPapers() {
        var school = insertSchool();
        var closedExam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var runningExam = insertExam(school, "Đang thi", "IN_PROGRESS");
        insertFailedSession(closedExam, SUBMITTED, 0);
        insertFailedSession(runningExam, SUBMITTED, 0);

        var exams = repository.findExamsAwaitingPublish(school, NOW, 5);

        assertThat(exams).hasSize(1);
        assertThat(exams.get(0).examId()).isEqualTo(closedExam);
        assertThat(exams.get(0).name()).isEqualTo("Giữa kỳ I");
        assertThat(exams.get(0).unscoredCount()).isEqualTo(1);
    }

    /** Kỳ nhiều bài trống nhất trước: đó là kỳ gây thiệt hại lớn nhất nếu bấm công bố nhầm. */
    @Test
    void shouldRankExamsByHowMuchWouldBeLostOnPublish() {
        var school = insertSchool();
        var small = insertExam(school, "Ít bài", "CLOSED");
        var big = insertExam(school, "Nhiều bài", "CLOSED");
        insertFailedSession(small, SUBMITTED, 0);
        insertFailedSession(big, SUBMITTED, 0);
        insertFailedSession(big, SUBMITTED, 1);
        insertAssignment(insertPendingReviewResult(big, insertSession(big, SUBMITTED, "GRADED")), null);

        var exams = repository.findExamsAwaitingPublish(school, NOW, 5);

        assertThat(exams).hasSize(2);
        assertThat(exams.get(0).name()).isEqualTo("Nhiều bài");
        assertThat(exams.get(0).unscoredCount()).isEqualTo(3);
        assertThat(exams.get(0).aiFailedRetryLeft()).isEqualTo(1);
        assertThat(exams.get(0).aiFailedNoRetryLeft()).isEqualTo(1);
        assertThat(exams.get(0).awaitingHumanGrading()).isEqualTo(1);
        assertThat(exams.get(1).unscoredCount()).isEqualTo(1);
    }

    /** Trường khác không được lẫn vào — mọi câu ở đây đều là phạm vi MỘT trường. */
    @Test
    void shouldNotLeakPapersFromAnotherSchool() {
        var school = insertSchool();
        var otherSchool = insertSchool();
        insertFailedSession(insertExam(school, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);
        insertFailedSession(insertExam(otherSchool, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);

        assertThat(repository.countUnscored(school, NOW).total()).isEqualTo(1);
        assertThat(repository.findExamsAwaitingPublish(school, NOW, 5)).hasSize(1);
    }

    /**
     * Con số trên thẻ và số dòng sau khi bấm vào phải bằng nhau. Đây là bất biến chính của cả cặp
     * truy vấn: cùng một CTE sinh ra cả hai, nên test này sẽ gãy ngay nếu ai đó viết lại vị từ ở một
     * bên.
     */
    @Test
    void shouldListExactlyThePapersTheDashboardCardCounts() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(exam, SUBMITTED, 0);
        insertFailedSession(exam, SUBMITTED, 1);
        // Đã chuyển người chấm: rời nhóm AI ở CẢ hai bên.
        insertPendingReviewResult(exam, insertFailedSession(exam, SUBMITTED, 0));

        var counted = repository.countUnscored(school, NOW).aiFailed();
        var listed = repository.findUnhandledAiFailures(school, null, null, 1, 20);

        assertThat(counted).isEqualTo(2);
        assertThat(listed.totalElements()).isEqualTo(2L);
        assertThat(listed.content()).hasSize(2);
    }

    /** Hai nhóm dẫn tới hai hành động khác nhau, nên lọc được giữa chúng là lý do màn này tồn tại. */
    @Test
    void shouldFilterByRemainingAiRetryAllowance() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(exam, SUBMITTED, 0);
        insertFailedSession(exam, SUBMITTED, 1);
        insertFailedSession(exam, SUBMITTED, 1);

        assertThat(repository.findUnhandledAiFailures(school, null, true, 1, 20).totalElements()).isEqualTo(1L);
        assertThat(repository.findUnhandledAiFailures(school, null, false, 1, 20).totalElements()).isEqualTo(2L);
        assertThat(repository.findUnhandledAiFailures(school, null, null, 1, 20).totalElements()).isEqualTo(3L);
        assertThat(repository.findUnhandledAiFailures(school, null, true, 1, 20).content().get(0).schoolRetryLeft())
            .isTrue();
    }

    /**
     * Hai số đếm đứng trên chính hai nút lọc, nên chúng phải theo bộ lọc kỳ thi — nếu lấy tổng toàn
     * trường thì nút ghi 3 mà bấm vào chỉ ra 1 dòng.
     */
    @Test
    void shouldCountAllowanceGroupsWithinTheSelectedExam() {
        var school = insertSchool();
        var examA = insertExam(school, "Giữa kỳ I", "CLOSED");
        var examB = insertExam(school, "Cuối kỳ", "CLOSED");
        insertFailedSession(examA, SUBMITTED, 0);
        insertFailedSession(examB, SUBMITTED, 0);
        insertFailedSession(examB, SUBMITTED, 1);

        assertThat(repository.countAiFailuresByAllowance(school, null)).containsExactly(2, 1);
        assertThat(repository.countAiFailuresByAllowance(school, examA)).containsExactly(1, 0);
        assertThat(repository.countAiFailuresByAllowance(school, examB)).containsExactly(1, 1);
    }

    /** Cũ nhất trước: em chờ lâu nhất là em cần xử lý trước, không phải sự cố mới nhất. */
    @Test
    void shouldListTheLongestWaitingPaperFirst() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(exam, Instant.parse("2026-08-27T02:14:00Z"), 0);
        var oldest = insertFailedSession(exam, Instant.parse("2026-08-20T01:00:00Z"), 0);

        var page = repository.findUnhandledAiFailures(school, null, null, 1, 20);

        assertThat(page.content().get(0).sessionId()).isEqualTo(oldest);
    }

    @Test
    void shouldCarryTheDetailsNeededToTriageAPaper() {
        var school = insertSchool();
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var sessionId = insertFailedSession(exam, SUBMITTED, 0);
        em.createNativeQuery("""
            UPDATE exam_sessions SET grading_error = :error, grading_retry_count = 3 WHERE id = :id
            """)
            .setParameter("error", "upstream timeout after 120s")
            .setParameter("id", sessionId)
            .executeUpdate();

        var row = repository.findUnhandledAiFailures(school, null, null, 1, 20).content().get(0);

        assertThat(row.examName()).isEqualTo("Giữa kỳ I");
        assertThat(row.candidateName()).isEqualTo("Trần Minh Anh");
        assertThat(row.failedAt()).isEqualTo(SUBMITTED);
        assertThat(row.error()).isEqualTo("upstream timeout after 120s");
        assertThat(row.aiRetryCount()).isEqualTo(3);
        assertThat(row.schoolRetryLeft()).isTrue();
    }

    /** Phiên hỏng qua nhánh DLT không mang lý do — đó là dữ liệu THẬT, không phải dòng lỗi. */
    @Test
    void shouldKeepPapersThatFailedWithoutAReason() {
        var school = insertSchool();
        insertFailedSession(insertExam(school, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);

        var row = repository.findUnhandledAiFailures(school, null, null, 1, 20).content().get(0);

        assertThat(row.error()).isNull();
        assertThat(row.aiRetryCount()).isNull();
        assertThat(row.className()).isNull();
    }

    @Test
    void shouldNotListPapersFromAnotherSchool() {
        var school = insertSchool();
        var otherSchool = insertSchool();
        insertFailedSession(insertExam(school, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);
        insertFailedSession(insertExam(otherSchool, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);

        assertThat(repository.findUnhandledAiFailures(school, null, null, 1, 20).totalElements()).isEqualTo(1L);
        assertThat(repository.countAiFailuresByAllowance(otherSchool, null)).containsExactly(1, 0);
    }

    // ---------- fixtures ----------

    private UUID insertSchool() {
        var id = UUID.randomUUID();
        var actor = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO schools (id, code, name, address, contact_email, contact_phone,
                                 is_active, student_count, created_at, updated_at, created_by, updated_by)
            VALUES (:id, :code, 'THPT Nguyễn Trãi', 'So 1', 'a@b.vn', '0900000000',
                    TRUE, 10, :now, :now, :actor, :actor)
            """)
            .setParameter("id", id)
            .setParameter("code", "C-" + id.toString().substring(0, 8))
            .setParameter("now", now)
            .setParameter("actor", actor)
            .executeUpdate();
        return id;
    }

    private UUID insertExam(UUID schoolId, String name, String status) {
        return insertExam(schoolId, name, status, "CENTRALIZED");
    }

    private UUID insertExam(UUID schoolId, String name, String status, String kind) {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO exams (id, school_id, language_id, code, name, kind, status, requires_otp,
                               close_at, created_at, updated_at)
            VALUES (:id, :schoolId, :languageId, :code, :name, :kind, :status, TRUE, :closeAt, :now, :now)
            """)
            .setParameter("id", id)
            .setParameter("schoolId", schoolId)
            .setParameter("languageId", UUID.randomUUID())
            .setParameter("code", "E-" + id.toString().substring(0, 8))
            .setParameter("name", name)
            .setParameter("kind", kind)
            .setParameter("status", status)
            .setParameter("closeAt", OffsetDateTime.ofInstant(SUBMITTED.plusSeconds(3600), ZoneOffset.UTC))
            .setParameter("now", now)
            .executeUpdate();
        return id;
    }

    private UUID insertCandidate(UUID examId) {
        var studentId = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO users (id, email, full_name, password_hash, date_of_birth, status, created_at, updated_at)
            VALUES (:id, :email, 'Trần Minh Anh', 'x', DATE '2008-01-01', 'ACTIVE', :now, :now)
            """)
            .setParameter("id", studentId)
            .setParameter("email", studentId + "@vox.test")
            .setParameter("now", now)
            .executeUpdate();

        var candidateId = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO exam_candidates (id, exam_id, student_id, status, assigned_at, updated_at)
            VALUES (:id, :examId, :studentId, 'ATTENDED', :now, :now)
            """)
            .setParameter("id", candidateId)
            .setParameter("examId", examId)
            .setParameter("studentId", studentId)
            .setParameter("now", now)
            .executeUpdate();
        return candidateId;
    }

    private UUID insertFailedSession(UUID examId, Instant submittedAt, int schoolRegradeCount) {
        var id = insertSession(examId, submittedAt, "GRADING_FAILED");
        em.createNativeQuery(
            "UPDATE exam_sessions SET school_regrade_count = :count WHERE id = :id")
            .setParameter("count", schoolRegradeCount)
            .setParameter("id", id)
            .executeUpdate();
        return id;
    }

    private UUID insertSession(UUID examId, Instant submittedAt, String status) {
        var id = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO exam_sessions (id, exam_id, candidate_id, paper_id, started_at, submitted_at,
                                       status, flagged)
            VALUES (:id, :examId, :candidateId, :paperId, :startedAt, :submittedAt, :status, FALSE)
            """)
            .setParameter("id", id)
            .setParameter("examId", examId)
            .setParameter("candidateId", insertCandidate(examId))
            .setParameter("paperId", UUID.randomUUID())
            .setParameter("startedAt", submittedAt.minusSeconds(1800))
            .setParameter("submittedAt", submittedAt)
            .setParameter("status", status)
            .executeUpdate();
        return id;
    }

    private UUID insertPendingReviewResult(UUID examId, UUID sessionId) {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO exam_candidate_results (id, exam_id, session_id, candidate_id, assessment_policy_id,
                                                framework_version_id, rubric_version_id, target_framework_band_id,
                                                policy_version, status, created_at, updated_at)
            VALUES (:id, :examId, :sessionId, :candidateId, :policyId, :frameworkVersionId, :rubricVersionId,
                    :bandId, 1, 'PENDING_REVIEW', :now, :now)
            """)
            .setParameter("id", id)
            .setParameter("examId", examId)
            .setParameter("sessionId", sessionId)
            .setParameter("candidateId", UUID.randomUUID())
            .setParameter("policyId", UUID.randomUUID())
            .setParameter("frameworkVersionId", UUID.randomUUID())
            .setParameter("rubricVersionId", UUID.randomUUID())
            .setParameter("bandId", UUID.randomUUID())
            .setParameter("now", now)
            .executeUpdate();
        return id;
    }

    /** {@code active_result_id} là cờ "phân công đang mở" — đó là thứ hai câu truy vấn LEFT JOIN vào. */
    private void insertAssignment(UUID candidateResultId, Instant deadlineAt) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO exam_grading_assignments (id, candidate_result_id, active_result_id, teacher_id,
                                                  round_type, status, assigned_at, deadline_at)
            VALUES (:id, :resultId, :resultId, :teacherId, 'INITIAL', 'ASSIGNED', :now, :deadlineAt)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("resultId", candidateResultId)
            .setParameter("teacherId", UUID.randomUUID())
            .setParameter("now", now)
            .setParameter("deadlineAt", deadlineAt == null
                ? null : OffsetDateTime.ofInstant(deadlineAt, ZoneOffset.UTC))
            .executeUpdate();
    }
}
