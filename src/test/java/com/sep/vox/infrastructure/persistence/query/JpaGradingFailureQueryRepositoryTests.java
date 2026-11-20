package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.config.ContainerTestConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT trên Postgres thật: gần như toàn bộ logic của repository này nằm trong những thứ
 * không mock được — hàm {@code vox_grading_error_signature} do V6 tạo ra,
 * {@code IS NOT DISTINCT FROM}, {@code COUNT(...) FILTER}, và {@code COUNT(DISTINCT ...)} bỏ qua
 * null. Một test với repository giả sẽ khẳng định đúng những gì nó tự dựng ra.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaGradingFailureQueryRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaGradingFailureQueryRepositoryTests extends ContainerTestConfig {

    private static final Instant WINDOW_FROM = Instant.parse("2026-08-17T00:00:00Z");
    private static final Instant WINDOW_TO = Instant.parse("2026-08-31T00:00:00Z");

    /** Hai thông điệp cùng sự cố, khác nhau ĐÚNG ở phần uuid và số — thứ chuẩn hóa phải xóa đi. */
    private static final String TIMEOUT_A =
        "Upstream AI service timed out after 30000ms for attempt 3f9a1c22-1111-4aaa-8bbb-1c2d3e4f5a6b";
    private static final String TIMEOUT_B =
        "Upstream AI service timed out after 45000ms for attempt 8c7b6a55-2222-4ccc-9ddd-9f8e7d6c5b4a";

    @Autowired
    private GradingFailureQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    @Test
    void shouldGroupTheSameIncidentTogetherDespiteDifferentIdsAndNumbers() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(exam, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(exam, Instant.parse("2026-08-23T03:48:00Z"), TIMEOUT_B, 3);

        var groups = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).sessionCount()).isEqualTo(2L);
        // Chữ ký đã thay uuid và số bằng chỗ trống, nên hai thông điệp về chung một nhóm.
        assertThat(groups.get(0).signature()).contains("<id>").contains("<n>");
        // Mẫu đại diện là thông điệp THÔ, để người trực còn đọc được một câu lỗi thật.
        assertThat(groups.get(0).sampleError()).isEqualTo(TIMEOUT_B);
        assertThat(groups.get(0).firstFailedAt()).isEqualTo(Instant.parse("2026-08-23T02:14:00Z"));
        assertThat(groups.get(0).lastFailedAt()).isEqualTo(Instant.parse("2026-08-23T03:48:00Z"));
    }

    /**
     * Phiên hỏng qua nhánh DLT không mang lý do nào. Đó là một NHÓM THẬT, không phải dữ liệu thiếu —
     * và nó phải tách khỏi nhóm có lý do, nếu không thì "không rõ nguyên nhân" bị trộn vào một sự cố
     * cụ thể và người trực đi sửa nhầm thứ.
     */
    @Test
    void shouldKeepSessionsWithoutAReasonAsTheirOwnGroup() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(exam, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(exam, Instant.parse("2026-08-24T02:14:00Z"), null, null);
        insertFailedSession(exam, Instant.parse("2026-08-25T02:14:00Z"), null, null);

        var groups = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10);

        assertThat(groups).hasSize(2);
        // Nhóm đông nhất trước: hai phiên không rõ nguyên nhân đứng trên một phiên timeout.
        assertThat(groups.get(0).signature()).isNull();
        assertThat(groups.get(0).sessionCount()).isEqualTo(2L);
        assertThat(groups.get(0).sampleError()).isNull();
        assertThat(groups.get(1).signature()).isNotNull();
    }

    /**
     * {@code COUNT(DISTINCT signature)} BỎ QUA null, nên nhóm không rõ nguyên nhân phải được cộng
     * tay — thiếu phép cộng đó thì một hệ thống mà mọi phiên đều hỏng qua DLT sẽ báo "0 nguyên nhân"
     * ngay bên trên một danh sách đang hiện một nhóm.
     */
    @Test
    void shouldCountTheUnknownCauseGroupAsACause() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(exam, Instant.parse("2026-08-24T02:14:00Z"), null, null);
        insertFailedSession(exam, Instant.parse("2026-08-25T02:14:00Z"), null, null);

        var totals = repository.countTotals(WINDOW_FROM, WINDOW_TO);

        assertThat(totals.sessionCount()).isEqualTo(2L);
        assertThat(totals.causeCount()).isEqualTo(1L);
    }

    /** Kỳ đã công bố điểm thì {@code RetryGradingExamSessionUseCase} từ chối, nên không tính là chấm lại được. */
    @Test
    void shouldNotCountSessionsOfPublishedExamsAsRetryable() {
        var school = insertSchool("THPT A");
        var openExam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var publishedExam = insertExam(school, "Thi thử", "RESULTS_PUBLISHED");
        insertFailedSession(openExam, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(publishedExam, Instant.parse("2026-08-23T02:20:00Z"), TIMEOUT_B, 3);

        var totals = repository.countTotals(WINDOW_FROM, WINDOW_TO);

        assertThat(totals.sessionCount()).isEqualTo(2L);
        assertThat(totals.retryableCount()).isEqualTo(1L);
        assertThat(repository.findGroups(WINDOW_FROM, WINDOW_TO, 10).get(0).retryableCount()).isEqualTo(1L);
    }

    /** Một trường dính hai nguyên nhân vẫn chỉ là MỘT trường bị ảnh hưởng. */
    @Test
    void shouldCountEachAffectedSchoolOnce() {
        var schoolA = insertSchool("THPT A");
        var schoolB = insertSchool("THPT B");
        var examA1 = insertExam(schoolA, "Giữa kỳ I", "DRAFT");
        var examA2 = insertExam(schoolA, "Cuối kỳ", "DRAFT");
        var examB = insertExam(schoolB, "Khảo sát", "DRAFT");
        insertFailedSession(examA1, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(examA2, Instant.parse("2026-08-24T02:14:00Z"), null, null);
        insertFailedSession(examB, Instant.parse("2026-08-25T02:14:00Z"), TIMEOUT_B, 3);

        var totals = repository.countTotals(WINDOW_FROM, WINDOW_TO);

        assertThat(totals.schoolCount()).isEqualTo(2L);
        assertThat(totals.causeCount()).isEqualTo(2L);
    }

    /**
     * Chữ ký null chọn ĐÚNG nhóm không rõ nguyên nhân, không phải "bỏ lọc". Dùng {@code =} thay cho
     * {@code IS NOT DISTINCT FROM} thì bấm vào nhóm đó ra danh sách rỗng trong khi thẻ vẫn hiện số.
     */
    @Test
    void shouldOpenTheUnknownCauseGroupWithANullSignature() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(exam, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(exam, Instant.parse("2026-08-24T02:14:00Z"), null, null);

        var page = repository.findSessions(WINDOW_FROM, WINDOW_TO, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.content().get(0).error()).isNull();
        assertThat(page.content().get(0).retryCount()).isNull();
    }

    @Test
    void shouldListSessionsOfOneGroupWithSchoolExamAndCandidate() {
        var school = insertSchool("THPT Nguyễn Huệ");
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var sessionId = insertFailedSession(exam, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);

        var signature = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10).get(0).signature();
        var page = repository.findSessions(WINDOW_FROM, WINDOW_TO, signature, 1, 20);

        assertThat(page.totalElements()).isEqualTo(1L);
        var row = page.content().get(0);
        assertThat(row.sessionId()).isEqualTo(sessionId);
        assertThat(row.schoolName()).isEqualTo("THPT Nguyễn Huệ");
        assertThat(row.examName()).isEqualTo("Giữa kỳ I");
        assertThat(row.candidateName()).isEqualTo("Trần Minh Anh");
        assertThat(row.retryCount()).isEqualTo(3);
        assertThat(row.retryable()).isTrue();
        // Chưa ai chuyển sang người chấm, nên chưa có dòng kết quả nào trỏ về phiên.
        assertThat(row.handedOff()).isFalse();
    }

    /**
     * Chuyển người chấm KHÔNG đổi trạng thái phiên (cố ý, xem {@code HandOffGradingToHumanUseCase}),
     * nên phiên vẫn nằm trong danh sách này. Cờ {@code handedOff} là thứ duy nhất phân biệt — thiếu
     * nó thì thao tác hàng loạt sẽ chấm đè lên bài giáo viên đang chấm dở.
     */
    @Test
    void shouldFlagSessionsAlreadyHandedOffToAHumanGrader() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "CLOSED");
        var sessionId = insertFailedSession(exam, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertCandidateResult(exam, sessionId);

        var signature = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10).get(0).signature();
        var page = repository.findSessions(WINDOW_FROM, WINDOW_TO, signature, 1, 20);

        assertThat(page.content().get(0).handedOff()).isTrue();
    }

    /** Cửa sổ là nửa mở {@code [from, to)} — cùng quy ước với thẻ trên trang tổng quan. */
    @Test
    void shouldExcludeSessionsOutsideTheWindow() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(exam, WINDOW_FROM.minusSeconds(1), TIMEOUT_A, 3);
        insertFailedSession(exam, WINDOW_TO, TIMEOUT_A, 3);
        insertFailedSession(exam, WINDOW_FROM, TIMEOUT_A, 3);

        assertThat(repository.countTotals(WINDOW_FROM, WINDOW_TO).sessionCount()).isEqualTo(1L);
    }

    /** Chỉ phiên GRADING_FAILED, không phải mọi phiên đã nộp. */
    @Test
    void shouldIgnoreSessionsThatAreNotFailed() {
        var school = insertSchool("THPT A");
        var exam = insertExam(school, "Giữa kỳ I", "DRAFT");
        insertSession(exam, Instant.parse("2026-08-23T02:14:00Z"), "GRADED", null, null);
        insertFailedSession(exam, Instant.parse("2026-08-23T02:15:00Z"), TIMEOUT_A, 3);

        assertThat(repository.countTotals(WINDOW_FROM, WINDOW_TO).sessionCount()).isEqualTo(1L);
    }

    // ---------- fixtures ----------

    private UUID insertSchool(String name) {
        var id = UUID.randomUUID();
        var actor = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO schools (id, code, name, address, contact_email, contact_phone,
                                 is_active, student_count, created_at, updated_at, created_by, updated_by)
            VALUES (:id, :code, :name, 'So 1', 'a@b.vn', '0900000000',
                    TRUE, 10, :now, :now, :actor, :actor)
            """)
            .setParameter("id", id)
            .setParameter("code", "C-" + id.toString().substring(0, 8))
            .setParameter("name", name)
            .setParameter("now", now)
            .setParameter("actor", actor)
            .executeUpdate();
        return id;
    }

    private UUID insertExam(UUID schoolId, String name, String status) {
        var id = UUID.randomUUID();
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO exams (id, school_id, language_id, code, name, kind, status, requires_otp,
                               created_at, updated_at)
            VALUES (:id, :schoolId, :languageId, :code, :name, 'CENTRALIZED', :status, TRUE, :now, :now)
            """)
            .setParameter("id", id)
            .setParameter("schoolId", schoolId)
            .setParameter("languageId", UUID.randomUUID())
            .setParameter("code", "E-" + id.toString().substring(0, 8))
            .setParameter("name", name)
            .setParameter("status", status)
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

    private UUID insertFailedSession(UUID examId, Instant submittedAt, String error, Integer retryCount) {
        return insertSession(examId, submittedAt, "GRADING_FAILED", error, retryCount);
    }

    private UUID insertSession(UUID examId, Instant submittedAt, String status, String error, Integer retryCount) {
        var id = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO exam_sessions (id, exam_id, candidate_id, paper_id, started_at, submitted_at,
                                       status, flagged, grading_error, grading_retry_count)
            VALUES (:id, :examId, :candidateId, :paperId, :startedAt, :submittedAt,
                    :status, FALSE, :error, :retryCount)
            """)
            .setParameter("id", id)
            .setParameter("examId", examId)
            .setParameter("candidateId", insertCandidate(examId))
            .setParameter("paperId", UUID.randomUUID())
            .setParameter("startedAt", submittedAt.minusSeconds(1800))
            .setParameter("submittedAt", submittedAt)
            .setParameter("status", status)
            .setParameter("error", error)
            .setParameter("retryCount", retryCount)
            .executeUpdate();
        return id;
    }

    /** Dòng kết quả PENDING_REVIEW mà HandOffGradingToHumanUseCase tạo ra khi giao bài cho người chấm. */
    private void insertCandidateResult(UUID examId, UUID sessionId) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO exam_candidate_results (id, exam_id, session_id, candidate_id, assessment_policy_id,
                                                framework_version_id, rubric_version_id, target_framework_band_id,
                                                policy_version, status, created_at, updated_at)
            VALUES (:id, :examId, :sessionId, :candidateId, :policyId, :frameworkVersionId, :rubricVersionId,
                    :bandId, 1, 'PENDING_REVIEW', :now, :now)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("examId", examId)
            .setParameter("sessionId", sessionId)
            .setParameter("candidateId", UUID.randomUUID())
            .setParameter("policyId", UUID.randomUUID())
            .setParameter("frameworkVersionId", UUID.randomUUID())
            .setParameter("rubricVersionId", UUID.randomUUID())
            .setParameter("bandId", UUID.randomUUID())
            .setParameter("now", now)
            .executeUpdate();
    }

    @SuppressWarnings("unused")
    private static Instant day(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
