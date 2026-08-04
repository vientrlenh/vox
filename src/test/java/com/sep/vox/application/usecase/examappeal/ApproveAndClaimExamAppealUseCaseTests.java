package com.sep.vox.application.usecase.examappeal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ApproveExamAppealCommand;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewerCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.usecase.examappeal.ApproveAndClaimExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.ApproveExamAppealUseCase;
import com.sep.vox.application.port.input.usecase.examappeal.AssignExamAppealReviewerUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;

public class ApproveAndClaimExamAppealUseCaseTests {

    private ApproveExamAppealUseCase approveExamAppealUseCase;
    private AssignExamAppealReviewerUseCase assignExamAppealReviewerUseCase;
    private ExamAppealAccessService examAppealAccessService;
    private ApproveAndClaimExamAppealUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();

    private AppealContext context;

    @BeforeEach
    void setUp() {
        approveExamAppealUseCase = mock(ApproveExamAppealUseCase.class);
        assignExamAppealReviewerUseCase = mock(AssignExamAppealReviewerUseCase.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        useCase = new ApproveAndClaimExamAppealUseCase(
            approveExamAppealUseCase, assignExamAppealReviewerUseCase, examAppealAccessService);

        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(ExamAppealStatus.PENDING);
        var candidateResult = new ExamCandidateResult();
        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);
        context = new AppealContext(appeal, candidateResult, new ExamSession(),
            UUID.randomUUID(), studentId, "Kiểm tra Nói giữa kỳ");

        when(examAppealAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(examAppealAccessService.load(appealId)).thenReturn(context);
        when(examAppealAccessService.isClassTestChair(context, teacherId)).thenReturn(true);
        when(assignExamAppealReviewerUseCase.execute(any())).thenReturn(assignmentId);
    }

    @Test
    void should_return_the_assignment_id_not_the_appeal_id() {
        assertThat(useCase.execute(appealId)).isEqualTo(assignmentId);
    }

    @Test
    void should_approve_before_assigning() {
        useCase.execute(appealId);

        var order = inOrder(approveExamAppealUseCase, assignExamAppealReviewerUseCase);
        order.verify(approveExamAppealUseCase).execute(any());
        order.verify(assignExamAppealReviewerUseCase).execute(any());
    }

    @Test
    void should_assign_the_current_teacher_as_reviewer_with_a_fixed_override_reason() {
        useCase.execute(appealId);

        var command = captureAssignCommand();
        assertThat(command.appealId()).isEqualTo(appealId);
        assertThat(command.reviewerId()).isEqualTo(teacherId);
        // Chuỗi cố định — Assign chỉ chấp nhận tự nhận chấm khi có override reason,
        // nên nó phải luôn khác null và luôn nói rõ đây là bài trên lớp.
        assertThat(command.overrideReason())
            .isEqualTo("Bài kiểm tra trên lớp: giáo viên phụ trách bài tự nhận chấm phúc khảo.");
    }

    @Test
    void should_default_the_deadline_to_three_days_later_at_5pm_vietnam_time() {
        useCase.execute(appealId);

        var expected = LocalDate.now(DateMapper.DEFAULT_INPUT_ZONE)
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
        when(examAppealAccessService.isClassTestChair(context, teacherId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(appealId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("giáo viên phụ trách bài");

        verify(approveExamAppealUseCase, never()).execute(any());
        verify(assignExamAppealReviewerUseCase, never()).execute(any());
    }

    private ApproveExamAppealCommand captureApproveCommand() {
        var captor = ArgumentCaptor.forClass(ApproveExamAppealCommand.class);
        verify(approveExamAppealUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private AssignExamAppealReviewerCommand captureAssignCommand() {
        var captor = ArgumentCaptor.forClass(AssignExamAppealReviewerCommand.class);
        verify(assignExamAppealReviewerUseCase).execute(captor.capture());
        return captor.getValue();
    }
}
