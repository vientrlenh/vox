package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.application.query.dto.AssessmentAwaitingPublishDto;
import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.RepositoryTest;

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
@RepositoryTest
@Import(JpaSchoolWorkloadQueryRepository.class)
class JpaSchoolWorkloadQueryRepositoryTests {

    private static final Instant NOW = Instant.parse("2026-09-01T03:00:00Z");
    private static final Instant SUBMITTED = Instant.parse("2026-08-27T02:14:00Z");

    @Autowired
    private SchoolWorkloadQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Five groups, one submission each: the total must equal 5 and no submission may be counted
     * twice.
     */
    @Test
    void shouldPartitionUnscoredWorkIntoFiveExclusiveBuckets() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");

        insertFailedSession(assessment, SUBMITTED, 0);
        insertFailedSession(assessment, SUBMITTED, 1);
        insertPendingReviewResult(assessment, insertSession(assessment, SUBMITTED, "GRADED"));
        insertAssignment(insertPendingReviewResult(assessment, insertSession(assessment, SUBMITTED, "GRADED")),
            NOW.minusSeconds(3600));
        insertAssignment(insertPendingReviewResult(assessment, insertSession(assessment, SUBMITTED, "GRADED")),
            NOW.plusSeconds(86400));

        SchoolUnscoredWorkloadDto counts = repository.countUnscored(school, NOW);

        assertThat(counts.aiFailedRetryLeft()).isEqualTo(1);
        assertThat(counts.aiFailedNoRetryLeft()).isEqualTo(1);
        assertThat(counts.awaitingAssignment()).isEqualTo(1);
        assertThat(counts.assignedOverdue()).isEqualTo(1);
        assertThat(counts.assignedInProgress()).isEqualTo(1);
        assertThat(counts.total()).isEqualTo(5);
        assertThat(counts.assessmentCount()).isEqualTo(1);
    }

    /**
     * Handing over to manual grading does NOT change the session status -- on purpose, see
     * {@code HandOffGradingToHumanUseCase}. The submission must leave the "AI failed, nobody has
     * handled it" group and move to the grader queue.
     */
    @Test
    void shouldMoveAHandedOffPaperOutOfTheUntouchedAiBucket() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID sessionId = insertFailedSession(assessment, SUBMITTED, 0);
        insertPendingReviewResult(assessment, sessionId);

        SchoolUnscoredWorkloadDto counts = repository.countUnscored(school, NOW);

        assertThat(counts.aiFailed()).isZero();
        assertThat(counts.awaitingAssignment()).isEqualTo(1);
        assertThat(counts.total()).isEqualTo(1);
    }

    /**
     * The school's per-attempt allowance decides whether a submission can still be saved by AI or
     * must be assigned to a teacher. These two numbers are what the school admin reads BEFORE
     * clicking publish scores.
     */
    @Test
    void shouldSplitFailedPapersByRemainingAiRetryAllowance() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(assessment, SUBMITTED, 0);
        insertFailedSession(assessment, SUBMITTED, 0);
        insertFailedSession(assessment, SUBMITTED, 1);

        SchoolUnscoredWorkloadDto counts = repository.countUnscored(school, NOW);

        assertThat(counts.aiFailedRetryLeft()).isEqualTo(2);
        assertThat(counts.aiFailedNoRetryLeft()).isEqualTo(1);
        assertThat(counts.aiFailed()).isEqualTo(3);
    }

    /**
     * A period whose scores are published has NO way out anymore (both use cases refuse in that
     * state), so its submissions are not work to do but damage already done. Leaving them in the
     * queue would give the number a floor that never reaches 0.
     */
    @Test
    void shouldExcludePapersOfAssessmentsWhoseResultsAreAlreadyPublished() {
        UUID school = insertSchool();
        UUID openAssessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID publishedAssessment = insertAssessment(school, "Thi thử", "RESULTS_PUBLISHED");
        UUID cancelledAssessment = insertAssessment(school, "Kỳ đã hủy", "CANCELLED");
        insertFailedSession(openAssessment, SUBMITTED, 0);
        insertFailedSession(publishedAssessment, SUBMITTED, 0);
        insertFailedSession(cancelledAssessment, SUBMITTED, 0);

        assertThat(repository.countUnscored(school, NOW).total()).isEqualTo(1);
    }

    /**
     * A class test has its own dispatch screen for the teacher who authored it -- merging it in
     * here would skew the numbers.
     */
    @Test
    void shouldIgnoreClassTests() {
        UUID school = insertSchool();
        UUID classTest = insertAssessment(school, "KT 15 phút", "CLOSED", "CLASS_TEST");
        insertFailedSession(classTest, SUBMITTED, 0);

        assertThat(repository.countUnscored(school, NOW).total()).isZero();
    }

    @Test
    void shouldReportTheOldestWaitingPaper() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(assessment, Instant.parse("2026-08-27T02:14:00Z"), 0);
        insertFailedSession(assessment, Instant.parse("2026-08-20T01:00:00Z"), 0);

        assertThat(repository.countUnscored(school, NOW).oldestSubmittedAt())
            .isEqualTo(Instant.parse("2026-08-20T01:00:00Z"));
    }

    /**
     * A clean queue must give null, not 0: 0 days of waiting means "there is a submission, just
     * submitted today".
     */
    @Test
    void shouldReturnNoOldestPaperWhenNothingIsWaiting() {
        UUID school = insertSchool();
        insertAssessment(school, "Giữa kỳ I", "CLOSED");

        SchoolUnscoredWorkloadDto counts = repository.countUnscored(school, NOW);

        assertThat(counts.total()).isZero();
        assertThat(counts.oldestSubmittedAt()).isNull();
    }

    /**
     * Only CLOSED periods -- exactly the set of periods for which the "Publish scores" button can
     * be clicked.
     */
    @Test
    void shouldListOnlyClosedAssessmentsThatStillHaveUnscoredPapers() {
        UUID school = insertSchool();
        UUID closedAssessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID runningAssessment = insertAssessment(school, "Đang thi", "IN_PROGRESS");
        insertFailedSession(closedAssessment, SUBMITTED, 0);
        insertFailedSession(runningAssessment, SUBMITTED, 0);

        List<AssessmentAwaitingPublishDto> assessments = repository.findAssessmentsAwaitingPublish(school, NOW, 5);

        assertThat(assessments).hasSize(1);
        assertThat(assessments.get(0).assessmentId()).isEqualTo(closedAssessment);
        assertThat(assessments.get(0).name()).isEqualTo("Giữa kỳ I");
        assertThat(assessments.get(0).unscoredCount()).isEqualTo(1);
    }

    /**
     * The period with the most empty submissions first: that is the one that does the most damage
     * if published by mistake.
     */
    @Test
    void shouldRankAssessmentsByHowMuchWouldBeLostOnPublish() {
        UUID school = insertSchool();
        UUID small = insertAssessment(school, "Ít bài", "CLOSED");
        UUID big = insertAssessment(school, "Nhiều bài", "CLOSED");
        insertFailedSession(small, SUBMITTED, 0);
        insertFailedSession(big, SUBMITTED, 0);
        insertFailedSession(big, SUBMITTED, 1);
        insertAssignment(insertPendingReviewResult(big, insertSession(big, SUBMITTED, "GRADED")), null);

        List<AssessmentAwaitingPublishDto> assessments = repository.findAssessmentsAwaitingPublish(school, NOW, 5);

        assertThat(assessments).hasSize(2);
        assertThat(assessments.get(0).name()).isEqualTo("Nhiều bài");
        assertThat(assessments.get(0).unscoredCount()).isEqualTo(3);
        assertThat(assessments.get(0).aiFailedRetryLeft()).isEqualTo(1);
        assertThat(assessments.get(0).aiFailedNoRetryLeft()).isEqualTo(1);
        assertThat(assessments.get(0).awaitingHumanGrading()).isEqualTo(1);
        assertThat(assessments.get(1).unscoredCount()).isEqualTo(1);
    }

    /** Another school must not be mixed in -- every query here is scoped to ONE school. */
    @Test
    void shouldNotLeakPapersFromAnotherSchool() {
        UUID school = insertSchool();
        UUID otherSchool = insertSchool();
        insertFailedSession(insertAssessment(school, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);
        insertFailedSession(insertAssessment(otherSchool, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);

        assertThat(repository.countUnscored(school, NOW).total()).isEqualTo(1);
        assertThat(repository.findAssessmentsAwaitingPublish(school, NOW, 5)).hasSize(1);
    }

    /**
     * The number on the card and the number of rows after clicking in must be equal. This is the
     * main invariant of the whole query pair: the same CTE produces both, so this test will break
     * right away if anyone rewrites the predicate on one side.
     */
    @Test
    void shouldListExactlyThePapersTheDashboardCardCounts() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(assessment, SUBMITTED, 0);
        insertFailedSession(assessment, SUBMITTED, 1);
        // Already handed to a grader: leaves the AI group on BOTH sides.
        insertPendingReviewResult(assessment, insertFailedSession(assessment, SUBMITTED, 0));

        int counted = repository.countUnscored(school, NOW).aiFailed();
        PageResult<SchoolGradingFailureDto> listed = repository.findUnhandledAiFailures(school, null, null, 1, 20);

        assertThat(counted).isEqualTo(2);
        assertThat(listed.totalElements()).isEqualTo(2L);
        assertThat(listed.content()).hasSize(2);
    }

    /**
     * The two groups lead to two different actions, so being able to filter between them is the
     * reason this screen exists.
     */
    @Test
    void shouldFilterByRemainingAiRetryAllowance() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(assessment, SUBMITTED, 0);
        insertFailedSession(assessment, SUBMITTED, 1);
        insertFailedSession(assessment, SUBMITTED, 1);

        assertThat(repository.findUnhandledAiFailures(school, null, true, 1, 20).totalElements()).isEqualTo(1L);
        assertThat(repository.findUnhandledAiFailures(school, null, false, 1, 20).totalElements()).isEqualTo(2L);
        assertThat(repository.findUnhandledAiFailures(school, null, null, 1, 20).totalElements()).isEqualTo(3L);
        assertThat(repository.findUnhandledAiFailures(school, null, true, 1, 20).content().get(0).schoolRetryLeft())
            .isTrue();
    }

    /**
     * The two counts sit on the two filter buttons themselves, so they must follow the assessment filter
     * -- if the school-wide total were used, a button saying 3 would show only 1 row when clicked.
     */
    @Test
    void shouldCountAllowanceGroupsWithinTheSelectedAssessment() {
        UUID school = insertSchool();
        UUID assessmentA = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID assessmentB = insertAssessment(school, "Cuối kỳ", "CLOSED");
        insertFailedSession(assessmentA, SUBMITTED, 0);
        insertFailedSession(assessmentB, SUBMITTED, 0);
        insertFailedSession(assessmentB, SUBMITTED, 1);

        assertThat(repository.countAiFailuresByAllowance(school, null)).containsExactly(2, 1);
        assertThat(repository.countAiFailuresByAllowance(school, assessmentA)).containsExactly(1, 0);
        assertThat(repository.countAiFailuresByAllowance(school, assessmentB)).containsExactly(1, 1);
    }

    /**
     * Oldest first: the student who has waited longest is the one to handle first, not the newest
     * incident.
     */
    @Test
    void shouldListTheLongestWaitingPaperFirst() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        insertFailedSession(assessment, Instant.parse("2026-08-27T02:14:00Z"), 0);
        UUID oldest = insertFailedSession(assessment, Instant.parse("2026-08-20T01:00:00Z"), 0);

        PageResult<SchoolGradingFailureDto> page = repository.findUnhandledAiFailures(school, null, null, 1, 20);

        assertThat(page.content().get(0).sessionId()).isEqualTo(oldest);
    }

    @Test
    void shouldCarryTheDetailsNeededToTriageAPaper() {
        UUID school = insertSchool();
        UUID assessment = insertAssessment(school, "Giữa kỳ I", "CLOSED");
        UUID sessionId = insertFailedSession(assessment, SUBMITTED, 0);
        em.createNativeQuery("""
            UPDATE assessment_sessions SET grading_error = :error, grading_retry_count = 3 WHERE id = :id
            """)
            .setParameter("error", "upstream timeout after 120s")
            .setParameter("id", sessionId)
            .executeUpdate();

        SchoolGradingFailureDto row = repository.findUnhandledAiFailures(school, null, null, 1, 20).content().get(0);

        assertThat(row.assessmentName()).isEqualTo("Giữa kỳ I");
        assertThat(row.candidateName()).isEqualTo("Trần Minh Anh");
        assertThat(row.failedAt()).isEqualTo(SUBMITTED);
        assertThat(row.error()).isEqualTo("upstream timeout after 120s");
        assertThat(row.aiRetryCount()).isEqualTo(3);
        assertThat(row.schoolRetryLeft()).isTrue();
    }

    /**
     * A session that failed through the DLT branch carries no reason -- that is REAL data, not an
     * error row.
     */
    @Test
    void shouldKeepPapersThatFailedWithoutAReason() {
        UUID school = insertSchool();
        insertFailedSession(insertAssessment(school, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);

        SchoolGradingFailureDto row = repository.findUnhandledAiFailures(school, null, null, 1, 20).content().get(0);

        assertThat(row.error()).isNull();
        assertThat(row.aiRetryCount()).isNull();
        assertThat(row.className()).isNull();
    }

    @Test
    void shouldNotListPapersFromAnotherSchool() {
        UUID school = insertSchool();
        UUID otherSchool = insertSchool();
        insertFailedSession(insertAssessment(school, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);
        insertFailedSession(insertAssessment(otherSchool, "Giữa kỳ I", "CLOSED"), SUBMITTED, 0);

        assertThat(repository.findUnhandledAiFailures(school, null, null, 1, 20).totalElements()).isEqualTo(1L);
        assertThat(repository.countAiFailuresByAllowance(otherSchool, null)).containsExactly(1, 0);
    }

    // ---------- fixtures ----------

    private UUID insertSchool() {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
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

    private UUID insertAssessment(UUID schoolId, String name, String status) {
        return insertAssessment(schoolId, name, status, "CENTRALIZED");
    }

    private UUID insertAssessment(UUID schoolId, String name, String status, String kind) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO assessments (id, school_id, language_id, code, name, kind, status, requires_otp,
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

    private UUID insertFailedSession(UUID assessmentId, Instant submittedAt, int schoolRegradeCount) {
        UUID id = insertSession(assessmentId, submittedAt, "GRADING_FAILED");
        em.createNativeQuery(
            "UPDATE assessment_sessions SET school_regrade_count = :count WHERE id = :id")
            .setParameter("count", schoolRegradeCount)
            .setParameter("id", id)
            .executeUpdate();
        return id;
    }

    private UUID insertSession(UUID assessmentId, Instant submittedAt, String status) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
            INSERT INTO assessment_sessions (id, assessment_id, candidate_id, paper_id, started_at, submitted_at,
                                       status, flagged)
            VALUES (:id, :assessmentId, :candidateId, :paperId, :startedAt, :submittedAt, :status, FALSE)
            """)
            .setParameter("id", id)
            .setParameter("assessmentId", assessmentId)
            .setParameter("candidateId", insertCandidate(assessmentId))
            .setParameter("paperId", UUID.randomUUID())
            .setParameter("startedAt", submittedAt.minusSeconds(1800))
            .setParameter("submittedAt", submittedAt)
            .setParameter("status", status)
            .executeUpdate();
        return id;
    }

    private UUID insertPendingReviewResult(UUID assessmentId, UUID sessionId) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO assessment_candidate_results (id, assessment_id, session_id, candidate_id, assessment_policy_id,
                                                framework_version_id, rubric_version_id, target_framework_band_id,
                                                policy_version, status, created_at, updated_at)
            VALUES (:id, :assessmentId, :sessionId, :candidateId, :policyId, :frameworkVersionId, :rubricVersionId,
                    :bandId, 1, 'PENDING_REVIEW', :now, :now)
            """)
            .setParameter("id", id)
            .setParameter("assessmentId", assessmentId)
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

    /**
     * {@code active_result_id} is the "assignment is open" flag -- that is what both queries LEFT
     * JOIN on.
     */
    private void insertAssignment(UUID candidateResultId, Instant deadlineAt) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        em.createNativeQuery("""
            INSERT INTO assessment_grading_assignments (id, candidate_result_id, active_result_id, teacher_id,
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
