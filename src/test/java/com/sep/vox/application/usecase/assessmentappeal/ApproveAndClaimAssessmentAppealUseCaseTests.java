package com.sep.vox.application.usecase.assessmentappeal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAndClaimAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.AssignAssessmentAppealReviewerUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentSession;

public class ApproveAndClaimAssessmentAppealUseCaseTests {

    private ApproveAssessmentAppealUseCase approveAssessmentAppealUseCase;
    private AssignAssessmentAppealReviewerUseCase assignAssessmentAppealReviewerUseCase;
    private AssessmentAppealAccessService assessmentAppealAccessService;
    private ApproveAndClaimAssessmentAppealUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    private AppealContext context;

    @BeforeEach
    void setUp() {
        approveAssessmentAppealUseCase = mock(ApproveAssessmentAppealUseCase.class);
        assignAssessmentAppealReviewerUseCase = mock(AssignAssessmentAppealReviewerUseCase.class);
        assessmentAppealAccessService = mock(AssessmentAppealAccessService.class);
        useCase = new ApproveAndClaimAssessmentAppealUseCase(
            approveAssessmentAppealUseCase, assignAssessmentAppealReviewerUseCase, assessmentAppealAccessService);

        AssessmentResultAppeal appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(AssessmentAppealStatus.PENDING);
        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);
        context = new AppealContext(appeal, candidateResult, new AssessmentSession(),
            UUID.randomUUID(), studentId, "Kiểm tra Nói giữa kỳ");

        when(assessmentAppealAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(assessmentAppealAccessService.load(appealId)).thenReturn(context);
        when(assessmentAppealAccessService.isClassTestChair(context, teacherId)).thenReturn(true);
        when(assignAssessmentAppealReviewerUseCase.execute(any())).thenReturn(assignmentId);
    }

    @Test
    void should_return_the_assignment_id_not_the_appeal_id() {
        assertThat(useCase.execute(appealId)).isEqualTo(assignmentId);
    }

    @Test
    void should_approve_before_assigning() {
        useCase.execute(appealId);

        InOrder order = inOrder(approveAssessmentAppealUseCase, assignAssessmentAppealReviewerUseCase);
        order.verify(approveAssessmentAppealUseCase).execute(any());
        order.verify(assignAssessmentAppealReviewerUseCase).execute(any());
    }

    @Test
    void should_assign_the_current_teacher_as_reviewer_with_a_fixed_override_reason() {
        useCase.execute(appealId);

        AssignAssessmentAppealReviewerCommand command = captureAssignCommand();
        assertThat(command.appealId()).isEqualTo(appealId);
        assertThat(command.reviewerId()).isEqualTo(teacherId);
        // A fixed string -- Assign only accepts a self-claim for grading when there is an override
        // reason, so it must always be non-null and must always make clear that this is a class
        // test.
        assertThat(command.overrideReason())
            .isEqualTo("Bài kiểm tra trên lớp: giáo viên phụ trách bài tự nhận chấm phúc khảo.");
    }

    @Test
    void should_default_the_deadline_to_three_days_later_at_5pm_vietnam_time() {
        useCase.execute(appealId);

        Instant expected = LocalDate.now(DateMapper.DEFAULT_INPUT_ZONE)
            .plusDays(3)
            .atTime(LocalTime.of(17, 0))
            .atZone(DateMapper.DEFAULT_INPUT_ZONE)
            .toInstant();
        assertThat(captureApproveCommand().deadline()).isEqualTo(expected);
    }

    @Test
    void should_use_the_same_deadline_for_the_appeal_and_the_grading_round() {
        useCase.execute(appealId);

        // Lệch nhau thì hạn hiển thị trên đơn và hạn chấm thật của giáo viên là hai
        // mốc khác nhau — không ai đối chiếu ra được.
        assertThat(captureAssignCommand().deadlineAt()).isEqualTo(captureApproveCommand().deadline());
    }

    @Test
    void should_reject_when_caller_is_not_the_class_test_chair() {
        when(assessmentAppealAccessService.isClassTestChair(context, teacherId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(appealId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("giáo viên phụ trách bài");

        verify(approveAssessmentAppealUseCase, never()).execute(any());
        verify(assignAssessmentAppealReviewerUseCase, never()).execute(any());
    }

    private ApproveAssessmentAppealCommand captureApproveCommand() {
        ArgumentCaptor<ApproveAssessmentAppealCommand> captor = ArgumentCaptor.forClass(ApproveAssessmentAppealCommand.class);
        verify(approveAssessmentAppealUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private AssignAssessmentAppealReviewerCommand captureAssignCommand() {
        ArgumentCaptor<AssignAssessmentAppealReviewerCommand> captor = ArgumentCaptor.forClass(AssignAssessmentAppealReviewerCommand.class);
        verify(assignAssessmentAppealReviewerUseCase).execute(captor.capture());
        return captor.getValue();
    }
}
