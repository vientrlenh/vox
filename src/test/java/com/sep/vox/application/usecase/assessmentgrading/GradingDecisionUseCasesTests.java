package com.sep.vox.application.usecase.assessmentgrading;

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

import com.sep.vox.application.event.AssessmentResultInvalidClearedPayloadV1;
import com.sep.vox.application.event.AssessmentResultInvalidatedPayloadV1;
import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.MissingResponseBackfillService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.DeclineGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.InvalidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.UpholdResultUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.AssessmentResultStatusHistoryRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Bốn hành động không nhập điểm, chạy với {@link GradingActionSupport} THẬT: phần đáng
 * kiểm ở đây là "quyết định này dẫn bài tới đâu", mà câu trả lời nằm ở chỗ nối giữa use
 * case và support — mock support đi thì chẳng còn gì để kiểm.
 */
class GradingDecisionUseCasesTests {

    private AssessmentGradingAccessService assessmentGradingAccessService;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentItemResponseRepository assessmentItemResponseRepository;
    private AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private MissingResponseBackfillService missingResponseBackfillService;
    private UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
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

    private AssessmentSession session;
    private AssessmentCandidate candidate;

    @BeforeEach
    void setUp() {
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        JsonSerializationPort jsonSerializationPort = OutboxTestSupport.jsonSerializationPort();

        support = new GradingActionSupport(
            assessmentGradingAccessService,
            assessmentCandidateResultRepository,
            assessmentCandidateRepository,
            assessmentGradingAssignmentRepository,
            mock(AssessmentResultAppealRepository.class),
            new ResultStatusHistoryRecorder(mock(AssessmentResultStatusHistoryRepository.class)),
            outboxRepository,
            jsonSerializationPort);

        // By default the submission is fully graded (the repo returns empty), so the fill-with-0
        // branch of Uphold does not run; a case that needs a submission with ungraded items calls
        // `givenAnsweredButUngradedItem()`.
        assessmentItemResponseRepository = mock(AssessmentItemResponseRepository.class);
        assessmentItemEvaluationRepository = mock(AssessmentItemEvaluationRepository.class);
        missingResponseBackfillService = mock(MissingResponseBackfillService.class);
        upsertAssessmentCandidateResultUseCase = mock(UpsertAssessmentCandidateResultUseCase.class);
        uphold = new UpholdResultUseCase(
            support,
            assessmentSessionRepository,
            assessmentItemResponseRepository,
            assessmentItemEvaluationRepository,
            missingResponseBackfillService,
            upsertAssessmentCandidateResultUseCase);
        invalidate = new InvalidateResultUseCase(support, outboxRepository, jsonSerializationPort);
        decline = new DeclineGradingAssignmentUseCase(support, outboxRepository, jsonSerializationPort);
        clearInvalid = new ClearInvalidResultUseCase(
            support, assessmentCandidateRepository, assessmentGradingAssignmentRepository,
            outboxRepository, jsonSerializationPort);

        session = new AssessmentSession();
        candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(assessmentGradingAssignmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private AssessmentCandidateResult given(GradingRoundType roundType, AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setSessionId(sessionId);
        result.setStatus(status);
        result.setTotalScore(new BigDecimal("6.00"));

        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(candidateResultId, teacherId, roundType, null,
            result.getTotalScore(), Instant.now().minus(4, ChronoUnit.DAYS), adminId,
            Instant.now().minus(1, ChronoUnit.DAYS));
        assignment.setId(assignmentId);
        when(assessmentGradingAccessService.loadForUpdate(assignmentId)).thenReturn(
            new GradingContext(assignment, result, session, UUID.randomUUID(), "IELTS Mock", AssessmentKind.CENTRALIZED));
        return result;
    }

    private GradingDecisionCommand command(String reason) {
        return new GradingDecisionCommand(assignmentId, reason);
    }

    /**
     * An item the candidate DID speak but nobody has graded -- exactly the state of a submission
     * that was forcibly ended during the assessment: the response is intact, but the evaluation never
     * existed.
     */
    private AssessmentItemResponse givenAnsweredButUngradedItem() {
        AssessmentItemResponse response = AssessmentItemResponse.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .paperItemId(UUID.randomUUID())
                .audioUrl("s3://audio")
                .durationSeconds(41)
                .transcript("I usually study at the library.")
                .terminationReason(null)
                .submittedAt(Instant.now())
                .build();
        when(assessmentItemResponseRepository.findBySessionId(sessionId)).thenReturn(java.util.List.of(response));
        when(assessmentItemEvaluationRepository.findByResponseIdIn(any())).thenReturn(java.util.List.of());
        when(missingResponseBackfillService.isSilentAnswer(response)).thenReturn(false);
        return response;
    }

    private <T> T capturePayload(String eventType, Class<T> type) {
        return OutboxTestSupport.capturePayload(outboxRepository, eventType, type);
    }

    // ---- UPHOLD -------------------------------------------------------------

    @Test
    void should_release_a_first_round_paper_when_the_score_is_upheld() {
        AssessmentCandidateResult result = given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        GradingActionResponse response = uphold.execute(command("AI chấm đúng"));

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RELEASED);
        assertThat(response.outcome()).isEqualTo(GradingOutcome.UPHELD.name());
    }

    @Test
    void should_clear_the_flag_when_a_human_upholds_outside_remediation() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);
        session.setFlagged(true);

        uphold.execute(command(null));

        assertThat(session.isFlagged()).isFalse();
        verify(assessmentSessionRepository).save(session);
    }

    @Test
    void should_keep_the_flag_when_remediation_confirms_the_violation() {
        AssessmentCandidateResult result = given(GradingRoundType.REMEDIATION, AssessmentCandidateResultStatus.INVALID);
        session.setFlagged(true);

        uphold.execute(command("Xác nhận vi phạm"));

        // Clearing the flag here would contradict the very decision that was just recorded.
        assertThat(session.isFlagged()).isTrue();
        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.INVALID);
        verify(assessmentSessionRepository, never()).save(any());
    }

    @Test
    void should_refuse_to_release_a_first_round_paper_that_still_has_answered_but_ungraded_items() {
        AssessmentCandidateResult result = given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);
        givenAnsweredButUngradedItem();

        // Keeping it unchanged in this round PUBLISHES the submission -- publishing a submission
        // that still has ungraded items means publishing an incomplete score, exactly the bug
        // measured on session 01a00e64.
        assertThatThrownBy(() -> uphold.execute(command("AI chấm đúng")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("chưa được chấm");

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
    }

    @Test
    void should_let_remediation_uphold_a_paper_whose_items_were_never_graded() {
        AssessmentCandidateResult result = given(GradingRoundType.REMEDIATION, AssessmentCandidateResultStatus.INVALID);
        givenAnsweredButUngradedItem();

        GradingActionResponse response = uphold.execute(command("Xác nhận vi phạm"));

        // A submission invalidated during the assessment has NO graded item at all -- and keeping it
        // unchanged in this round only confirms the invalidation and publishes nothing. Demanding
        // full grading first would lock the teacher into an assignment with no way to close it.
        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.INVALID);
        assertThat(response.outcome()).isEqualTo(GradingOutcome.UPHELD.name());
        // The submission stays at INVALID, so there is nothing to fill in or recalculate.
        verify(missingResponseBackfillService, never()).recordSilentAnswer(any(), any());
        verify(upsertAssessmentCandidateResultUseCase, never()).execute(any(), any());
    }

    // ---- INVALIDATE ---------------------------------------------------------

    @Test
    void should_invalidate_the_result_and_tell_the_student_why() {
        AssessmentCandidateResult result = given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        invalidate.execute(command("Gian lận: đọc bài mẫu"));

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.INVALID);
        assertThat(result.getFinalizedAt()).isNotNull();
        AssessmentResultInvalidatedPayloadV1 payload = capturePayload(
            EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED, AssessmentResultInvalidatedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
        assertThat(payload.reason()).isEqualTo("Gian lận: đọc bài mẫu");
    }

    @Test
    void should_refuse_to_invalidate_without_a_reason() {
        AssessmentCandidateResult result = given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> invalidate.execute(command("  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lý do");

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
    }

    // ---- DECLINE ------------------------------------------------------------

    @Test
    void should_return_the_paper_to_the_queue_without_changing_its_status() {
        AssessmentCandidateResult result = given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

        decline.execute(command("Quen thí sinh"));

        // The submission does not change status -- it just returns to the unassigned row.
        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
        AssessmentGradingAssignment assignment = assessmentGradingAccessService.loadForUpdate(assignmentId).assignment();
        assertThat(assignment.isCompleted()).isTrue();
        assertThat(assignment.getOutcome()).isEqualTo(GradingOutcome.DECLINED);
        assertThat(assignment.getActiveResultId()).isNull();
    }

    @Test
    void should_notify_the_admin_who_made_the_assignment_not_the_student() {
        given(GradingRoundType.INITIAL, AssessmentCandidateResultStatus.PENDING_REVIEW);

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
        AssessmentCandidateResult result = given(GradingRoundType.REMEDIATION, AssessmentCandidateResultStatus.INVALID);
        candidate.setBlockedAt(Instant.now().minus(2, ChronoUnit.DAYS));

        clearInvalid.execute(command("Không có vi phạm"));

        assertThat(result.getStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
        // If the block is not lifted, every later score recalculation would pull the submission
        // back to INVALID.
        assertThat(candidate.getBlockedAt()).isNull();
        capturePayload(
            EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED, AssessmentResultInvalidClearedPayloadV1.class);
    }

    @Test
    void should_open_the_new_round_without_inheriting_the_expired_deadline() {
        given(GradingRoundType.REMEDIATION, AssessmentCandidateResultStatus.INVALID);

        clearInvalid.execute(command("Không có vi phạm"));

        ArgumentCaptor<AssessmentGradingAssignment> captor = ArgumentCaptor.forClass(AssessmentGradingAssignment.class);
        verify(assessmentGradingAssignmentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        AssessmentGradingAssignment reopened = captor.getAllValues().stream()
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
        AssessmentCandidateResult result = given(GradingRoundType.REMEDIATION, AssessmentCandidateResultStatus.INVALID);
        result.setFinalizedAt(Instant.now().minus(2, ChronoUnit.DAYS));

        clearInvalid.execute(command("Không có vi phạm"));

        assertThat(result.getFinalizedAt()).isNull();
    }
}
