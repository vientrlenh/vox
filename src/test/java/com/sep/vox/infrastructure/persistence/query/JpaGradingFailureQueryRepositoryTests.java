package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Chạy SQL THẬT trên Postgres thật: gần như toàn bộ logic của repository này nằm trong những thứ
 * không mock được — hàm {@code vox_grading_error_signature} do V6 tạo ra,
 * {@code IS NOT DISTINCT FROM}, {@code COUNT(...) FILTER}, và {@code COUNT(DISTINCT ...)} bỏ qua
 * null. Một test với repository giả sẽ khẳng định đúng những gì nó tự dựng ra.
 */
@RepositoryTest
@Import(JpaGradingFailureQueryRepository.class)
class JpaGradingFailureQueryRepositoryTests {

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
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(assessment, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(assessment, Instant.parse("2026-08-23T03:48:00Z"), TIMEOUT_B, 3);

        List<GradingFailureGroupDto> groups = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).sessionCount()).isEqualTo(2L);
        // The signature has replaced uuids and numbers with placeholders, so the two messages fall
        // into one group.
        assertThat(groups.get(0).signature()).contains("<id>").contains("<n>");
        // The representative sample is the RAW message, so whoever is on call can still read a real
        // error sentence.
        assertThat(groups.get(0).sampleError()).isEqualTo(TIMEOUT_B);
        assertThat(groups.get(0).firstFailedAt()).isEqualTo(Instant.parse("2026-08-23T02:14:00Z"));
        assertThat(groups.get(0).lastFailedAt()).isEqualTo(Instant.parse("2026-08-23T03:48:00Z"));
    }

    /**
     * A session that fails through the DLT branch carries no reason. That is a REAL GROUP, not
     * missing data -- and it must be separated from the groups that have a reason, otherwise "cause
     * unknown" gets mixed into one specific incident and the on-call person fixes the wrong thing.
     */
    @Test
    void shouldKeepSessionsWithoutAReasonAsTheirOwnGroup() {
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(assessment, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(assessment, Instant.parse("2026-08-24T02:14:00Z"), null, null);
        insertFailedSession(assessment, Instant.parse("2026-08-25T02:14:00Z"), null, null);

        List<GradingFailureGroupDto> groups = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10);

        assertThat(groups).hasSize(2);
        // The biggest group first: two sessions with unknown cause rank above one timeout session.
        assertThat(groups.get(0).signature()).isNull();
        assertThat(groups.get(0).sessionCount()).isEqualTo(2L);
        assertThat(groups.get(0).sampleError()).isNull();
        assertThat(groups.get(1).signature()).isNotNull();
    }

    /**
     * {@code COUNT(DISTINCT signature)} IGNORES null, so the unknown-cause group must be added by
     * hand -- without that addition, a system where every session failed through the DLT would
     * report "0 causes" right above a list that is showing one group.
     */
    @Test
    void shouldCountTheUnknownCauseGroupAsACause() {
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(assessment, Instant.parse("2026-08-24T02:14:00Z"), null, null);
        insertFailedSession(assessment, Instant.parse("2026-08-25T02:14:00Z"), null, null);

        GradingFailureTotalsDto totals = repository.countTotals(WINDOW_FROM, WINDOW_TO);

        assertThat(totals.sessionCount()).isEqualTo(2L);
        assertThat(totals.causeCount()).isEqualTo(1L);
    }

    /**
     * A period whose scores are already published is refused by
     * {@code RetryGradingAssessmentSessionUseCase}, so it does not count as regradable.
     */
    @Test
    void shouldNotCountSessionsOfPublishedAssessmentsAsRetryable() {
        UUID school = insertSchool("THPT A");
        UUID openAssessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID publishedAssessment = insertAssessment(school, "Thi thử", "RESULTS_PUBLISHED");
        insertFailedSession(openAssessment, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(publishedAssessment, Instant.parse("2026-08-23T02:20:00Z"), TIMEOUT_B, 3);

        GradingFailureTotalsDto totals = repository.countTotals(WINDOW_FROM, WINDOW_TO);

        assertThat(totals.sessionCount()).isEqualTo(2L);
        assertThat(totals.retryableCount()).isEqualTo(1L);
        assertThat(repository.findGroups(WINDOW_FROM, WINDOW_TO, 10).get(0).retryableCount()).isEqualTo(1L);
    }

    /** A school hit by two causes is still only ONE affected school. */
    @Test
    void shouldCountEachAffectedSchoolOnce() {
        UUID schoolA = insertSchool("THPT A");
        UUID schoolB = insertSchool("THPT B");
        UUID assessmentA1 = insertAssessment(schoolA, "Giữa kỳ I", "DRAFT");
        UUID assessmentA2 = insertAssessment(schoolA, "Cuối kỳ", "DRAFT");
        UUID assessmentB = insertAssessment(schoolB, "Khảo sát", "DRAFT");
        insertFailedSession(assessmentA1, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(assessmentA2, Instant.parse("2026-08-24T02:14:00Z"), null, null);
        insertFailedSession(assessmentB, Instant.parse("2026-08-25T02:14:00Z"), TIMEOUT_B, 3);

        GradingFailureTotalsDto totals = repository.countTotals(WINDOW_FROM, WINDOW_TO);

        assertThat(totals.schoolCount()).isEqualTo(2L);
        assertThat(totals.causeCount()).isEqualTo(2L);
    }

    /**
     * A null signature selects EXACTLY the unknown-cause group, it does not mean "remove the
     * filter". Using {@code =} instead of {@code IS NOT DISTINCT FROM} would make clicking that
     * group produce an empty list while the card still shows a number.
     */
    @Test
    void shouldOpenTheUnknownCauseGroupWithANullSignature() {
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(assessment, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertFailedSession(assessment, Instant.parse("2026-08-24T02:14:00Z"), null, null);

        PageResult<GradingFailureSessionDto> page = repository.findSessions(WINDOW_FROM, WINDOW_TO, null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.content().get(0).error()).isNull();
        assertThat(page.content().get(0).retryCount()).isNull();
    }

    @Test
    void shouldListSessionsOfOneGroupWithSchoolAssessmentAndCandidate() {
        UUID school = insertSchool("THPT Nguyễn Huệ");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID sessionId = insertFailedSession(assessment, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);

        String signature = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10).get(0).signature();
        PageResult<GradingFailureSessionDto> page = repository.findSessions(WINDOW_FROM, WINDOW_TO, signature, 1, 20);

        assertThat(page.totalElements()).isEqualTo(1L);
        GradingFailureSessionDto row = page.content().get(0);
        assertThat(row.sessionId()).isEqualTo(sessionId);
        assertThat(row.schoolName()).isEqualTo("THPT Nguyễn Huệ");
        assertThat(row.assessmentName()).isEqualTo("Giữa kỳ I");
        assertThat(row.candidateName()).isEqualTo("Trần Minh Anh");
        assertThat(row.retryCount()).isEqualTo(3);
        assertThat(row.retryable()).isTrue();
        // Nobody has handed over to a grader yet, so no result row points back to the session.
        assertThat(row.handedOff()).isFalse();
    }

    /**
     * Handing over to a grader does NOT change the session status (on purpose, see
     * {@code HandOffGradingToHumanUseCase}), so the session stays in this list. The
     * {@code handedOff} flag is the only thing that tells them apart -- without it, a bulk action
     * would grade over a submission the teacher is in the middle of grading.
     */
    @Test
    void shouldFlagSessionsAlreadyHandedOffToAHumanGrader() {
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID sessionId = insertFailedSession(assessment, Instant.parse("2026-08-23T02:14:00Z"), TIMEOUT_A, 3);
        insertCandidateResult(assessment, sessionId);

        String signature = repository.findGroups(WINDOW_FROM, WINDOW_TO, 10).get(0).signature();
        PageResult<GradingFailureSessionDto> page = repository.findSessions(WINDOW_FROM, WINDOW_TO, signature, 1, 20);

        assertThat(page.content().get(0).handedOff()).isTrue();
    }

    /**
     * The window is half-open {@code [from, to)} -- the same convention as the card on the overview
     * page.
     */
    @Test
    void shouldExcludeSessionsOutsideTheWindow() {
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "DRAFT");
        insertFailedSession(assessment, WINDOW_FROM.minusSeconds(1), TIMEOUT_A, 3);
        insertFailedSession(assessment, WINDOW_TO, TIMEOUT_A, 3);
        insertFailedSession(assessment, WINDOW_FROM, TIMEOUT_A, 3);

        assertThat(repository.countTotals(WINDOW_FROM, WINDOW_TO).sessionCount()).isEqualTo(1L);
    }

    /** Only GRADING_FAILED sessions, not every submitted session. */
    @Test
    void shouldIgnoreSessionsThatAreNotFailed() {
        UUID school = insertSchool("THPT A");
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "DRAFT");
        insertSession(assessment, Instant.parse("2026-08-23T02:14:00Z"), "GRADED", null, null);
        insertFailedSession(assessment, Instant.parse("2026-08-23T02:15:00Z"), TIMEOUT_A, 3);

        assertThat(repository.countTotals(WINDOW_FROM, WINDOW_TO).sessionCount()).isEqualTo(1L);
    }

    // ---------- fixtures ----------

    private UUID insertSchool(String name) {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
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

    private UUID insertAssessment(UUID schoolId, String name, String status) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO assessments (id, school_id, language_id, code, name, kind, status, requires_otp,
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

    private UUID insertCandidate(UUID assessmentId) {
        UUID studentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO users (id, email, full_name, password_hash, date_of_birth, status, created_at, updated_at)
            VALUES (:id, :email, 'Trần Minh Anh', 'x', DATE '2008-01-01', 'ACTIVE', :now, :now)
            """)
            .setParameter("id", studentId)
            .setParameter("email", studentId + "@vox.test")
            .setParameter("now", now)
            .executeUpdate();

        UUID candidateId = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO assessment_candidates (id, assessment_id, student_id, status, assigned_at, updated_at)
            VALUES (:id, :assessmentId, :studentId, 'ATTENDED', :now, :now)
            """)
            .setParameter("id", candidateId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("studentId", studentId)
            .setParameter("now", now)
            .executeUpdate();
        return candidateId;
    }

    private UUID insertFailedSession(UUID assessmentId, Instant submittedAt, String error, Integer retryCount) {
        return insertSession(assessmentId, submittedAt, "GRADING_FAILED", error, retryCount);
    }

    private UUID insertSession(UUID assessmentId, Instant submittedAt, String status, String error, Integer retryCount) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO assessment_sessions (id, assessment_id, candidate_id, paper_id, started_at, submitted_at,
                                       status, flagged, grading_error, grading_retry_count)
            VALUES (:id, :assessmentId, :candidateId, :paperId, :startedAt, :submittedAt,
                    :status, FALSE, :error, :retryCount)
            """)
            .setParameter("id", id)
            .setParameter("assessmentId", assessmentId)
            .setParameter("candidateId", insertCandidate(assessmentId))
            .setParameter("paperId", UUID.randomUUID())
            .setParameter("startedAt", submittedAt.minusSeconds(1800))
            .setParameter("submittedAt", submittedAt)
            .setParameter("status", status)
            .setParameter("error", error)
            .setParameter("retryCount", retryCount)
            .executeUpdate();
        return id;
    }

    /**
     * The PENDING_REVIEW result row that HandOffGradingToHumanUseCase creates when it gives the
     * submission to a grader.
     */
    private void insertCandidateResult(UUID assessmentId, UUID sessionId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO assessment_candidate_results (id, assessment_id, session_id, candidate_id, assessment_policy_id,
                                                framework_version_id, rubric_version_id, target_framework_band_id,
                                                policy_version, status, created_at, updated_at)
            VALUES (:id, :assessmentId, :sessionId, :candidateId, :policyId, :frameworkVersionId, :rubricVersionId,
                    :bandId, 1, 'PENDING_REVIEW', :now, :now)
            """)
            .setParameter("id", UUID.randomUUID())
            .setParameter("assessmentId", assessmentId)
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
