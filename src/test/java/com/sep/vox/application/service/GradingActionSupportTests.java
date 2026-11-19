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

import com.sep.vox.application.event.ExamResultRegradedPayloadV1;
import com.sep.vox.application.event.ExamResultReleasedPayloadV1;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;

/**
 * Phần dùng chung của bốn hành động: kiểm luật ở đầu, và đổi trạng thái + audit +
 * thông báo ở cuối. Đây là chỗ duy nhất ghi nhật ký, nên nếu nó bỏ sót thì cả tính
 * năng audit coi như không tồn tại.
 */
class GradingActionSupportTests {

    private ExamGradingAccessService examGradingAccessService;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private ExamResultStatusHistoryRepository examResultStatusHistoryRepository;
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
        examGradingAccessService = mock(ExamGradingAccessService.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examResultStatusHistoryRepository = mock(ExamResultStatusHistoryRepository.class);
        outboxRepository = mock(OutboxRepository.class);

        // The recorder uses the REAL implementation: "is an audit entry written or not" is exactly
        // what is being checked.
        support = new GradingActionSupport(
            examGradingAccessService,
            examCandidateResultRepository,
            examCandidateRepository,
            examGradingAssignmentRepository,
            examResultAppealRepository,
            new ResultStatusHistoryRecorder(examResultStatusHistoryRepository),
            outboxRepository,
            OutboxTestSupport.jsonSerializationPort());

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);

        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
    }

    private ExamCandidateResult result(ExamCandidateResultStatus status, String totalScore) {
        ExamCandidateResult result = new ExamCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setStatus(status);
        result.setTotalScore(totalScore == null ? null : new BigDecimal(totalScore));
        return result;
    }

    private GradingContext context(GradingRoundType roundType, ExamCandidateResult result) {
        return context(roundType, result, null);
    }

    private GradingContext context(
            GradingRoundType roundType, ExamCandidateResult result, UUID linkedAppealId) {
        ExamGradingAssignment assignment = ExamGradingAssignment.open(candidateResultId, teacherId, roundType, linkedAppealId,
            result.getTotalScore(), Instant.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        return new GradingContext(assignment, result, new ExamSession(), UUID.randomUUID(), "IELTS Mock", ExamKind.CENTRALIZED);
    }

    /**
     * An appeal round comes with an appeal; the appeal is in GRADING because the teacher is holding
     * the submission.
     */
    private ExamResultAppeal givenAppealRound(ExamCandidateResult result) {
        when(examGradingAccessService.loadForUpdate(assignmentId))
            .thenReturn(context(GradingRoundType.APPEAL, result, appealId));
        ExamResultAppeal appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(ExamAppealStatus.GRADING);
        appeal.setScoreBefore(result.getTotalScore());
        when(examResultAppealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
        return appeal;
    }

    private void given(GradingRoundType roundType, ExamCandidateResult result) {
        when(examGradingAccessService.loadForUpdate(assignmentId)).thenReturn(context(roundType, result));
    }

    private ExamResultStatusHistory captureHistory() {
        ArgumentCaptor<ExamResultStatusHistory> captor = ArgumentCaptor.forClass(ExamResultStatusHistory.class);
        verify(examResultStatusHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- prepare: kiểm luật trước khi động vào dữ liệu ----------------------

    @Test
    void should_reject_an_action_that_does_not_belong_to_the_round() {
        given(GradingRoundType.INITIAL, result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00"));

        assertThatThrownBy(() ->
            support.prepare(assignmentId, GradingOutcome.CLEARED_INVALID, "lý do"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không hợp lệ với vòng chấm");
    }

    @Test
    void should_reject_when_the_result_has_left_the_rounds_status() {
        // Bài bị luồng khác kéo đi (phúc khảo, chốt sổ) trong lúc giáo viên mở màn chấm.
        given(GradingRoundType.INITIAL, result(ExamCandidateResultStatus.RELEASED, "6.00"));

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.UPHELD, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không còn ở trạng thái");
    }

    @Test
    void should_reject_a_completed_assignment() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        ExamGradingAccessService.GradingContext context = context(GradingRoundType.INITIAL, result);
        context.assignment().complete(GradingOutcome.UPHELD, null, Instant.now());
        when(examGradingAccessService.loadForUpdate(assignmentId)).thenReturn(context);

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.UPHELD, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chốt");
    }

    @Test
    void should_require_a_reason_where_the_policy_demands_one() {
        given(GradingRoundType.INITIAL, result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00"));

        assertThatThrownBy(() -> support.prepare(assignmentId, GradingOutcome.INVALIDATED, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");
    }

    // ---- finish: trạng thái, audit, sự kiện --------------------------------

    @Test
    void should_release_the_result_and_close_the_assignment() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(result.getReleasedAt()).isNotNull();
        ExamGradingAssignment assignment = prepared.context().assignment();
        assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.COMPLETED);
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.UPHELD);
        // Releases the slot so the submission can receive the next round -- this is how the unique
        // index works.
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_publish_a_first_release_notification_but_not_a_change_one() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        ExamResultReleasedPayloadV1 payload = OutboxTestSupport.capturePayload(
            outboxRepository, EventTypeConstant.EXAM_RESULT_RELEASED, ExamResultReleasedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
    }

    @Test
    void should_not_notify_when_a_spot_check_keeps_the_score() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        // A post-check keeps the score unchanged: bothering the student with "your score just
        // changed" would be wrong.
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_notify_when_a_spot_check_changes_a_published_score() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.REGRADED, null);
        result.setTotalScore(new BigDecimal("7.50"));

        support.finish(prepared, result);

        ExamResultRegradedPayloadV1 payload = OutboxTestSupport.capturePayload(
            outboxRepository, EventTypeConstant.EXAM_RESULT_REGRADED, ExamResultRegradedPayloadV1.class);
        assertThat(payload.scoreBefore()).isEqualByComparingTo("6.00");
        assertThat(payload.scoreAfter()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_record_the_status_change_with_the_round_as_source() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, "ghi chú");

        support.finish(prepared, result);

        ExamResultStatusHistory history = captureHistory();
        assertThat(history.getFromStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
        assertThat(history.getToStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(history.getSource()).isEqualTo(ResultStatusChangeSource.TEACHER_INITIAL);
        assertThat(history.getActorId()).isEqualTo(teacherId);
        assertThat(history.getReason()).isEqualTo("ghi chú");
    }

    @Test
    void should_still_record_an_upheld_decision_that_changes_nothing() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, "AI chấm đúng");

        support.finish(prepared, result);

        // Neither status nor score changes -- but the teacher HAS made a decision, and that
        // decision is what proves a person has looked at the submission.
        ExamResultStatusHistory history = captureHistory();
        assertThat(history.getFromStatus()).isEqualTo(history.getToStatus());
        assertThat(history.getReason()).isEqualTo("AI chấm đúng");
    }

    @Test
    void should_record_the_invalidation_reason() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.INVALIDATED, "Gian lận: đọc bài mẫu");

        support.finish(prepared, result);

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(result.getFinalizedAt()).isNotNull();
        assertThat(captureHistory().getReason()).isEqualTo("Gian lận: đọc bài mẫu");
    }

    // ---- vòng phúc khảo: số phận của đơn ------------------------------------

    @Test
    void should_move_the_appeal_back_to_approved_when_the_reviewer_declines() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.RE_GRADING, "6.00");
        ExamResultAppeal appeal = givenAppealRound(result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.DECLINED, "Quen thí sinh");

        support.finish(prepared, result);

        // When the teacher sends the submission back, the appeal is still waiting for SOMEONE ELSE,
        // not finished. APPROVED is the only status AssignExamAppealReviewerUseCase accepts.
        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.APPROVED);
        assertThat(appeal.getResolvedAt()).isNull();
        verify(examResultAppealRepository).save(appeal);
    }

    @Test
    void should_publish_the_appeal_when_the_reviewer_decides() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.RE_GRADING, "6.00");
        ExamResultAppeal appeal = givenAppealRound(result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.REGRADED, "Chấm lại phần 2");
        result.setTotalScore(new BigDecimal("7.00"));

        support.finish(prepared, result);

        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.PUBLISHED);
        assertThat(appeal.getScoreAfter()).isEqualByComparingTo("7.00");
        assertThat(appeal.getResolvedBy()).isEqualTo(teacherId);
        assertThat(appeal.getResolvedAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
    }

    @Test
    void should_not_touch_the_appeal_when_the_round_is_not_an_appeal() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.PENDING_REVIEW, "6.00");
        given(GradingRoundType.INITIAL, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        verify(examResultAppealRepository, never()).findById(any());
    }

    // ---- dấu vết trạng thái không được để lại rác ---------------------------

    @Test
    void should_clear_the_finalized_mark_when_a_result_returns_to_pending_review() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.INVALID, "0.00");
        result.setFinalizedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        given(GradingRoundType.REMEDIATION, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.CLEARED_INVALID, "Không vi phạm");

        support.finish(prepared, result);

        // A PENDING_REVIEW submission carrying finalized_at is a contradiction: it has not
        // finalized anything.
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
        assertThat(result.getFinalizedAt()).isNull();
    }

    @Test
    void should_save_the_result_only_when_the_status_actually_changes() {
        ExamCandidateResult result = result(ExamCandidateResultStatus.RELEASED, "6.00");
        given(GradingRoundType.SPOT_CHECK, result);
        GradingActionSupport.PreparedAction prepared = support.prepare(assignmentId, GradingOutcome.UPHELD, null);

        support.finish(prepared, result);

        verify(examCandidateResultRepository, never()).save(eq(result));
    }
}
