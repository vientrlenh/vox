package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.sep.vox.application.event.AssessmentResultInvalidClearedPayloadV1;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.service.GradingActionSupport.PreparedAction;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;

/**
 * Hành động phức tạp nhất trong bốn: nó vừa gỡ chặn thí sinh, vừa đổi trạng thái bài,
 * vừa tự mở một vòng chấm mới — và cả ba việc phải xong trong cùng một transaction.
 */
class ClearInvalidResultUseCaseTests {

    private GradingActionSupport gradingActionSupport;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private OutboxRepository outboxRepository;
    private ClearInvalidResultUseCase useCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    private AssessmentCandidateResult result;
    private AssessmentCandidate candidate;

    @BeforeEach
    void setUp() {
        gradingActionSupport = mock(GradingActionSupport.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        useCase = new ClearInvalidResultUseCase(gradingActionSupport, assessmentCandidateRepository,
            assessmentGradingAssignmentRepository, outboxRepository, OutboxTestSupport.jsonSerializationPort());

        result = new AssessmentCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setStatus(AssessmentCandidateResultStatus.INVALID);

        candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        candidate.setBlockedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(candidateResultId, teacherId,
            GradingRoundType.REMEDIATION, null, null, Instant.now(), UUID.randomUUID(), null);
        assignment.setId(assignmentId);
        AssessmentGradingAccessService.GradingContext context = new GradingContext(
            assignment, result, new AssessmentSession(), UUID.randomUUID(), "IELTS Mock", AssessmentKind.CENTRALIZED);
        when(gradingActionSupport.prepare(any(), any(), any())).thenReturn(new PreparedAction(
            context, teacherId, GradingRoundType.REMEDIATION, GradingOutcome.CLEARED_INVALID,
            "Xem lại video, không có vi phạm", null));
        when(gradingActionSupport.resolveStudentId(any())).thenReturn(studentId);
        when(assessmentGradingAssignmentRepository.save(any())).thenAnswer(invocation -> {
            AssessmentGradingAssignment saved = (AssessmentGradingAssignment) invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    private GradingDecisionCommand command() {
        return new GradingDecisionCommand(assignmentId, "Xem lại video, không có vi phạm");
    }

    private AssessmentGradingAssignment captureReopened() {
        ArgumentCaptor<AssessmentGradingAssignment> captor = ArgumentCaptor.forClass(AssessmentGradingAssignment.class);
        verify(assessmentGradingAssignmentRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_unblock_the_candidate() {
        useCase.execute(command());

        // resolveDefaultStatus ép INVALID khi thí sinh còn bị chặn; không gỡ thì mọi
        // lần tính lại điểm sau này sẽ âm thầm huỷ quyết định của giáo viên.
        assertThat(candidate.getBlockedAt()).isNull();
        verify(assessmentCandidateRepository).save(candidate);
    }

    @Test
    void should_open_exactly_one_initial_round_for_the_same_teacher() {
        GradingActionResponse response = useCase.execute(command());

        AssessmentGradingAssignment reopened = captureReopened();
        assertThat(reopened.getRoundType()).isEqualTo(GradingRoundType.INITIAL);
        assertThat(reopened.getTeacherId()).isEqualTo(teacherId);
        assertThat(reopened.getCandidateResultId()).isEqualTo(candidateResultId);
        // The submission does not fall into the auto-assign pool without an owner -> closes the
        // BE-1 review scenario.
        assertThat(reopened.getActiveResultId()).isEqualTo(candidateResultId);
        assertThat(response.nextAssignmentId()).isEqualTo(reopened.getId());
    }

    @Test
    void should_close_the_remediation_round_before_opening_the_next_one() {
        useCase.execute(command());

        // Mandatory order: finish() releases the old row's active_result_id first, and only then is
        // the new row inserted -- the reverse violates the unique index.
        InOrder inOrder = org.mockito.Mockito.inOrder(gradingActionSupport, assessmentGradingAssignmentRepository);
        inOrder.verify(gradingActionSupport).finish(any(), any());
        inOrder.verify(assessmentGradingAssignmentRepository).save(any());
    }

    @Test
    void should_notify_the_student_that_the_paper_is_restored() {
        useCase.execute(command());

        AssessmentResultInvalidClearedPayloadV1 payload = OutboxTestSupport.capturePayload(outboxRepository,
            EventTypeConstant.ASSESSMENT_RESULT_INVALID_CLEARED, AssessmentResultInvalidClearedPayloadV1.class);
        assertThat(payload.studentId()).isEqualTo(studentId);
        assertThat(payload.reason()).isEqualTo("Xem lại video, không có vi phạm");
    }

    @Test
    void should_not_touch_the_candidate_when_it_was_never_blocked() {
        candidate.setBlockedAt(null);

        useCase.execute(command());

        verify(assessmentCandidateRepository, never()).save(any());
    }
}
