package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.AssessmentResultRegradedPayloadV1;
import com.sep.vox.application.event.AssessmentResultReleasedPayloadV1;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.AssessmentResultStatusHistoryRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;

/**
 * Phần dùng chung của bốn hành động: kiểm luật ở đầu, và đổi trạng thái + audit +
 * thông báo ở cuối. Đây là chỗ duy nhất ghi nhật ký, nên nếu nó bỏ sót thì cả tính
 * năng audit coi như không tồn tại.
 */
class GradingActionSupportTests {

    private AssessmentGradingAccessService assessmentGradingAccessService;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentResultStatusHistoryRepository assessmentResultStatusHistoryRepository;
    private OutboxRepository outboxRepository;
    private GradingActionSupport support;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentResultStatusHistoryRepository = mock(AssessmentResultStatusHistoryRepository.class);
        outboxRepository = mock(OutboxRepository.class);

        // The recorder uses the REAL implementation: "is an audit entry written or not" is exactly
        // what is being checked.
        support = new GradingActionSupport(
            assessmentGradingAccessService,
            assessmentCandidateResultRepository,
            assessmentCandidateRepository,
            assessmentGradingAssignmentRepository,
            assessmentResultAppealRepository,
            new ResultStatusHistoryRecorder(assessmentResultStatusHistoryRepository),
            outboxRepository,
            OutboxTestSupport.jsonSerializationPort());

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(teacherId);

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
    }

    private AssessmentCandidateResult result(AssessmentCandidateResultStatus status, String totalScore) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setStatus(status);
        result.setTotalScore(totalScore == null ? null : new BigDecimal(totalScore));
        return result;
    }

    private GradingContext context(GradingRoundType roundType, AssessmentCandidateResult result) {
        return context(roundType, result, null);
    }

    private GradingContext context(
            GradingRoundType roundType, AssessmentCandidateResult result, UUID linkedAppealId) {
        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(candidateResultId, teacherId, roundType, linkedAppealId,
            result.getTotalScore(), Instant.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        return new GradingContext(assignment, result, new AssessmentSession(), UUID.randomUUID(), "IELTS Mock", AssessmentKind.CENTRALIZED);
    }

    /**
     * An appeal round comes with an appeal; the appeal is in GRADING because the teacher is holding
     * the submission.
     */
    private AssessmentResultAppeal givenAppealRound(AssessmentCandidateResult result) {
        when(assessmentGradingAccessService.loadForUpdate(assignmentId))
            .thenReturn(context(GradingRoundType.APPEAL, result, appealId));
        AssessmentResultAppeal appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(AssessmentAppealStatus.GRADING);
        appeal.setScoreBefore(result.getTotalScore());
        when(assessmentResultAppealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
        return appeal;
    }

    private void given(GradingRoundType roundType, AssessmentCandidateResult result) {
        when(assessmentGradingAccessService.loadForUpdate(assignmentId)).thenReturn(context(roundType, result));
    }

    private AssessmentResultStatusHistory captureHistory() {
        ArgumentCaptor<AssessmentResultStatusHistory> captor = ArgumentCaptor.forClass(AssessmentResultStatusHistory.class);
        verify(assessmentResultStatusHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- prepare: kiểm luật trước khi động vào dữ liệu ----------------------

    @Test
    void should_reject_an_action_that_does_not_belong_to_the_round() {
        given(GradingRoundType.INITIAL, result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00"));

        assertThatThrownBy(() ->
            support.prepare(assignmentId, GradingOutcome.CLEARED_INVALID, "lý do"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ với vòng chấm");
    }

    @Test
    void should_reject_when_the_result_has_left_the_rounds_status() {
        // Bài bị luồng khác kéo đi (phúc khảo, chốt sổ) trong lúc giáo viên mở màn chấm.
        given(GradingRoundType.INITIAL, result(AssessmentCandidateResultStatus.RELEASED, "6.00"));

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.UPHELD, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không còn ở trạng thái");
    }

    @Test
    void should_reject_a_completed_assignment() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00");
        AssessmentGradingAccessService.GradingContext context = context(GradingRoundType.INITIAL, result);
        context.assignment().complete(GradingOutcome.UPHELD, null, Instant.now());
        when(assessmentGradingAccessService.loadForUpdate(assignmentId)).thenReturn(context);

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.UPHELD, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chốt");
    }

    @Test
    void should_require_a_reason_where_the_policy_demands_one() {
        given(GradingRoundType.INITIAL, result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00"));

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.INVALIDATED, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");
    }

    // ---- finish: trạng thái, audit, sự kiện --------------------------------

    @Test
    void should_release_the_result_and_close_the_assignment() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        assertThat(result.getReleasedAt()).isNotNull();
        AssessmentGradingAssignment assignment = prepared.context().assignment();
        assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.COMPLETED);
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.UPHELD);
        // Releases the slot so the submission can receive the next round -- this is how the unique
        // index works.
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_publish_a_first_release_notification_but_not_a_change_one() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        AssessmentResultReleasedPayloadV1 payload = OutboxTestSupport.capturePayload(
            outboxRepository, EventTypeConstant.ASSESSMENT_RESULT_RELEASED, AssessmentResultReleasedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
    }

    @Test
    void should_not_notify_when_a_spot_check_keeps_the_score() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        // A post-check keeps the score unchanged: bothering the student with "your score just
        // changed" would be wrong.
        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_notify_when_a_spot_check_changes_a_published_score() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.REGRADED, null);
        result.setTotalScore(new BigDecimal("7.50"));

        support.finish(prepared, result);

        AssessmentResultRegradedPayloadV1 payload = OutboxTestSupport.capturePayload(
            outboxRepository, EventTypeConstant.ASSESSMENT_RESULT_REGRADED, AssessmentResultRegradedPayloadV1.class);
        assertThat(payload.scoreBefore()).isEqualByComparingTo("6.00");
        assertThat(payload.scoreAfter()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_record_the_status_change_with_the_round_as_source() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, "ghi chú");

        support.finish(prepared, result);

        AssessmentResultStatusHistory history = captureHistory();
        assertThat(history.getFromStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
        assertThat(history.getToStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.TEACHER_INITIAL);
        assertThat(history.getActorId()).isEqualTo(teacherId);
        assertThat(history.getReason()).isEqualTo("ghi chú");
    }

    @Test
    void should_still_record_an_upheld_decision_that_changes_nothing() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, "AI chấm đúng");

        support.finish(prepared, result);

        // Neither status nor score changes -- but the teacher HAS made a decision, and that
        // decision is what proves a person has looked at the submission.
        AssessmentResultStatusHistory history = captureHistory();
        assertThat(history.getFromStatus()).isEqualTo(history.getToStatus());
        assertThat(history.getReason()).isEqualTo("AI chấm đúng");
    }

    @Test
    void should_record_the_invalidation_reason() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.INVALIDATED, "Gian lận: đọc bài mẫu");

        support.finish(prepared, result);

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.INVALID);
        assertThat(result.getFinalizedAt()).isNotNull();
        assertThat(captureHistory().getReason()).isEqualTo("Gian lận: đọc bài mẫu");
    }

    // ---- vòng phúc khảo: số phận của đơn ------------------------------------

    @Test
    void should_move_the_appeal_back_to_approved_when_the_reviewer_declines() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.RE_GRADING, "6.00");
        AssessmentResultAppeal appeal = givenAppealRound(result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.DECLINED, "Quen thí sinh");

        support.finish(prepared, result);

        // When the teacher sends the submission back, the appeal is still waiting for SOMEONE ELSE,
        // not finished. APPROVED is the only status AssignAssessmentAppealReviewerUseCase accepts.
        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.APPROVED);
        assertThat(appeal.getResolvedAt()).isNull();
        verify(assessmentResultAppealRepository).save(appeal);
    }

    @Test
    void should_publish_the_appeal_when_the_reviewer_decides() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.RE_GRADING, "6.00");
        AssessmentResultAppeal appeal = givenAppealRound(result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.REGRADED, "Chấm lại phần 2");
        result.setTotalScore(new BigDecimal("7.00"));

        support.finish(prepared, result);

        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.PUBLISHED);
        assertThat(appeal.getScoreAfter()).isEqualByComparingTo("7.00");
        assertThat(appeal.getResolvedBy()).isEqualTo(teacherId);
        assertThat(appeal.getResolvedAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
    }

    @Test
    void should_not_touch_the_appeal_when_the_round_is_not_an_appeal() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        verify(assessmentResultAppealRepository, never()).findById(any());
    }

    // ---- dấu vết trạng thái không được để lại rác ---------------------------

    @Test
    void should_clear_the_finalized_mark_when_a_result_returns_to_pending_review() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.INVALID, "0.00");
        result.setFinalizedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        given(GradingRoundType.REMEDIATION, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.CLEARED_INVALID, "Không vi phạm");

        support.finish(prepared, result);

        // A PENDING_REVIEW submission carrying finalized_at is a contradiction: it has not
        // finalized anything.
        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
        assertThat(result.getFinalizedAt()).isNull();
    }

    @Test
    void should_save_the_result_only_when_the_status_actually_changes() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        verify(assessmentCandidateResultRepository, never()).save(eq(result));
    }
}
