package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.sep.vox.application.event.ExamResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.ExamResultInvalidatedPayloadV1;
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.MissingResponseBackfillService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.DeclineGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.InvalidateResultUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.UpholdResultUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Bốn hành động không nhập điểm, chạy với {@link GradingActionSupport} THẬT: phần đáng
 * kiểm ở đây là "quyết định này dẫn bài tới đâu", mà câu trả lời nằm ở chỗ nối giữa use
 * case và support — mock support đi thì chẳng còn gì để kiểm.
 */
class GradingDecisionUseCasesTests {

    private ExamGradingAccessService examGradingAccessService;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamSessionRepository examSessionRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private MissingResponseBackfillService missingResponseBackfillService;
    private UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private OutboxRepository outboxRepository;
    private GradingActionSupport support;

    private UpholdResultUseCase uphold;
    private InvalidateResultUseCase invalidate;
    private DeclineGradingAssignmentUseCase decline;
    private ClearInvalidResultUseCase clearInvalid;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    private ExamSession session;
    private ExamCandidate candidate;

    @BeforeEach
    void setUp() {
        examGradingAccessService = mock(ExamGradingAccessService.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        JsonSerializationPort jsonSerializationPort = OutboxTestSupport.jsonSerializationPort();

        support = new GradingActionSupport(
            examGradingAccessService,
            examCandidateResultRepository,
            examCandidateRepository,
            examGradingAssignmentRepository,
            mock(ExamResultAppealRepository.class),
            new ResultStatusHistoryRecorder(mock(ExamResultStatusHistoryRepository.class)),
            outboxRepository,
            jsonSerializationPort);

        // By default the submission is fully graded (the repo returns empty), so the fill-with-0
        // branch of Uphold does not run; a case that needs a submission with ungraded items calls
        // `givenAnsweredButUngradedItem()`.
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        missingResponseBackfillService = mock(MissingResponseBackfillService.class);
        upsertExamCandidateResultUseCase = mock(UpsertExamCandidateResultUseCase.class);
        uphold = new UpholdResultUseCase(
            support,
            examSessionRepository,
            examItemResponseRepository,
            examItemEvaluationRepository,
            missingResponseBackfillService,
            upsertExamCandidateResultUseCase);
        invalidate = new InvalidateResultUseCase(support, outboxRepository, jsonSerializationPort);
        decline = new DeclineGradingAssignmentUseCase(support, outboxRepository, jsonSerializationPort);
        clearInvalid = new ClearInvalidResultUseCase(
            support, examCandidateRepository, examGradingAssignmentRepository,
            outboxRepository, jsonSerializationPort);

        session = new ExamSession();
        candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(examCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(examGradingAssignmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private ExamCandidateResult given(GradingRoundType roundType, ExamCandidateResultStatus status) {
        ExamCandidateResult result = new ExamCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setSessionId(sessionId);
        result.setStatus(status);
        result.setTotalScore(new BigDecimal("6.00"));

        ExamGradingAssignment assignment = ExamGradingAssignment.open(candidateResultId, teacherId, roundType, null,
            result.getTotalScore(), Instant.now().minus(4, ChronoUnit.DAYS), adminId,
            Instant.now().minus(1, ChronoUnit.DAYS));
        assignment.setId(assignmentId);
        when(examGradingAccessService.loadForUpdate(assignmentId)).thenReturn(
            new GradingContext(assignment, result, session, UUID.randomUUID(), "IELTS Mock", ExamKind.CENTRALIZED));
        return result;
    }

    private GradingDecisionCommand command(String reason) {
        return new GradingDecisionCommand(assignmentId, reason);
    }

    /**
     * An item the candidate DID speak but nobody has graded -- exactly the state of a submission
     * that was forcibly ended during the exam: the response is intact, but the evaluation never
     * existed.
     */
    private ExamItemResponse givenAnsweredButUngradedItem() {
        ExamItemResponse response = new ExamItemResponse(
            UUID.randomUUID(), sessionId, UUID.randomUUID(), "s3://audio", 41,
            "I usually study at the library.", null, Instant.now());
        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(java.util.List.of(response));
        when(examItemEvaluationRepository.findByResponseIdIn(any())).thenReturn(java.util.List.of());
        when(missingResponseBackfillService.isSilentAnswer(response)).thenReturn(false);
        return response;
    }

    private <T> T capturePayload(String eventType, Class<T> type) {
        return OutboxTestSupport.capturePayload(outboxRepository, eventType, type);
    }

    // ---- UPHOLD -------------------------------------------------------------

    @Test
    void should_release_a_first_round_paper_when_the_score_is_upheld() {
        ExamCandidateResult result = given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        GradingActionResponse response = uphold.execute(command("AI chấm đúng"));

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.RELEASED);
        assertThat(response.outcome()).isEqualTo(GradingOutcome.UPHELD.name());
    }

    @Test
    void should_clear_the_flag_when_a_human_upholds_outside_remediation() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);
        session.setFlagged(true);

        uphold.execute(command(null));

        assertThat(session.isFlagged()).isFalse();
        verify(examSessionRepository).save(session);
    }

    @Test
    void should_keep_the_flag_when_remediation_confirms_the_violation() {
        ExamCandidateResult result = given(GradingRoundType.REMEDIATION, ExamCandidateResultStatus.INVALID);
        session.setFlagged(true);

        uphold.execute(command("Xác nhận vi phạm"));

        // Clearing the flag here would contradict the very decision that was just recorded.
        assertThat(session.isFlagged()).isTrue();
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        verify(examSessionRepository, never()).save(any());
    }

    @Test
    void should_refuse_to_release_a_first_round_paper_that_still_has_answered_but_ungraded_items() {
        ExamCandidateResult result = given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);
        givenAnsweredButUngradedItem();

        // Keeping it unchanged in this round PUBLISHES the submission -- publishing a submission
        // that still has ungraded items means publishing an incomplete score, exactly the bug
        // measured on session 01a00e64.
        assertThatThrownBy(() -> uphold.execute(command("AI chấm đúng")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa được chấm");

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
    }

    @Test
    void should_let_remediation_uphold_a_paper_whose_items_were_never_graded() {
        ExamCandidateResult result = given(GradingRoundType.REMEDIATION, ExamCandidateResultStatus.INVALID);
        givenAnsweredButUngradedItem();

        GradingActionResponse response = uphold.execute(command("Xác nhận vi phạm"));

        // A submission invalidated during the exam has NO graded item at all -- and keeping it
        // unchanged in this round only confirms the invalidation and publishes nothing. Demanding
        // full grading first would lock the teacher into an assignment with no way to close it.
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(response.outcome()).isEqualTo(GradingOutcome.UPHELD.name());
        // The submission stays at INVALID, so there is nothing to fill in or recalculate.
        verify(missingResponseBackfillService, never()).recordSilentAnswer(any(), any());
        verify(upsertExamCandidateResultUseCase, never()).execute(any(), any());
    }

    // ---- INVALIDATE ---------------------------------------------------------

    @Test
    void should_invalidate_the_result_and_tell_the_student_why() {
        ExamCandidateResult result = given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        invalidate.execute(command("Gian lận: đọc bài mẫu"));

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(result.getFinalizedAt()).isNotNull();
        ExamResultInvalidatedPayloadV1 payload = capturePayload(
            EventTypeConstant.EXAM_RESULT_INVALIDATED, ExamResultInvalidatedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
        assertThat(payload.reason()).isEqualTo("Gian lận: đọc bài mẫu");
    }

    @Test
    void should_refuse_to_invalidate_without_a_reason() {
        ExamCandidateResult result = given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> invalidate.execute(command("  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
    }

    // ---- DECLINE ------------------------------------------------------------

    @Test
    void should_return_the_paper_to_the_queue_without_changing_its_status() {
        ExamCandidateResult result = given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        decline.execute(command("Quen thí sinh"));

        // The submission does not change status -- it just returns to the unassigned row.
        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
        ExamGradingAssignment assignment = examGradingAccessService.loadForUpdate(assignmentId).assignment();
        assertThat(assignment.isCompleted()).isTrue();
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_notify_the_admin_who_made_the_assignment_not_the_student() {
        given(GradingRoundType.INITIAL, ExamCandidateResultStatus.PENDING_REVIEW);

        decline.execute(command("Quá tải"));

        GradingAssignmentDeclinedPayloadV1 payload = capturePayload(
            EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, GradingAssignmentDeclinedPayloadV1.class);
        assertThat(payload.assignedBy()).isEqualTo(adminId);
        assertThat(payload.teacherId()).isEqualTo(teacherId);
        assertThat(payload.reason()).isEqualTo("Quá tải");
    }

    // ---- CLEAR_INVALID ------------------------------------------------------

    @Test
    void should_unblock_the_candidate_and_reopen_a_first_round() {
        ExamCandidateResult result = given(GradingRoundType.REMEDIATION, ExamCandidateResultStatus.INVALID);
        candidate.setBlockedAt(Instant.now().minus(2, ChronoUnit.DAYS));

        clearInvalid.execute(command("Không có vi phạm"));

        assertThat(result.getStatus()).isEqualTo(ExamCandidateResultStatus.PENDING_REVIEW);
        // If the block is not lifted, every later score recalculation would pull the submission
        // back to INVALID.
        assertThat(candidate.getBlockedAt()).isNull();
        capturePayload(
            EventTypeConstant.EXAM_RESULT_INVALID_CLEARED, ExamResultInvalidClearedPayloadV1.class);
    }

    @Test
    void should_open_the_new_round_without_inheriting_the_expired_deadline() {
        given(GradingRoundType.REMEDIATION, ExamCandidateResultStatus.INVALID);

        clearInvalid.execute(command("Không có vi phạm"));

        ArgumentCaptor<ExamGradingAssignment> captor = ArgumentCaptor.forClass(ExamGradingAssignment.class);
        verify(examGradingAssignmentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        ExamGradingAssignment reopened = captor.getAllValues().stream()
            .filter(assignment -> assignment.getRoundType() == GradingRoundType.INITIAL)
            .findFirst()
            .orElseThrow();
        // The deadline of the REMEDIATION round has already passed; inheriting it would make the
        // new assignment show red "overdue" the moment it is created and land in the reclaim list
        // before anyone can grade it.
        assertThat(reopened.getDeadlineAt()).isNull();
        assertThat(reopened.getTeacherId()).isEqualTo(teacherId);
        assertThat(reopened.getActiveResultId()).isEqualTo(candidateResultId);
    }

    @Test
    void should_clear_the_finalized_mark_when_the_paper_goes_back_to_pending_review() {
        ExamCandidateResult result = given(GradingRoundType.REMEDIATION, ExamCandidateResultStatus.INVALID);
        result.setFinalizedAt(Instant.now().minus(2, ChronoUnit.DAYS));

        clearInvalid.execute(command("Không có vi phạm"));

        assertThat(result.getFinalizedAt()).isNull();
    }
}
