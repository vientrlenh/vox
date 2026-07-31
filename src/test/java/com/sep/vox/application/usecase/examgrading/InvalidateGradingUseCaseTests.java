package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.InvalidateGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.examgrading.InvalidateGradingUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

public class InvalidateGradingUseCaseTests {

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamGradingAccessService examGradingAccessService;
    private InvalidateGradingUseCase useCase;

    private final UUID assignmentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new InvalidateGradingUseCase(
            examCandidateResultRepository, examGradingAssignmentRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
    }

    private GradingContext given(
            GradingAssignmentStatus assignmentStatus, ExamCandidateResultStatus resultStatus, boolean flagged) {
        var assignment = new ExamGradingAssignment(
            assignmentId, candidateResultId, teacherId, assignmentStatus, Instant.now(), null, null);

        var candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setSessionId(sessionId);
        candidateResult.setStatus(resultStatus);

        var session = new ExamSession();
        session.setId(sessionId);
        session.setFlagged(flagged);

        var context = new GradingContext(
            assignment, candidateResult, session, UUID.randomUUID(), "IELTS Speaking Mock");
        when(examGradingAccessService.loadForGrading(assignmentId, null)).thenReturn(context);
        return context;
    }

    private InvalidateGradingCommand command() {
        return new InvalidateGradingCommand(assignmentId, null, "Có người thứ hai nhắc bài");
    }

    @Test
    void should_invalidate_result_and_complete_assignment() {
        var context = given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, true);

        var response = useCase.execute(command());

        assertThat(response.candidateResultId()).isEqualTo(candidateResultId);
        assertThat(response.resultStatus()).isEqualTo("INVALID");
        assertThat(context.candidateResult().getStatus()).isEqualTo(ExamCandidateResultStatus.INVALID);
        assertThat(context.candidateResult().getFinalizedAt()).isNotNull();
        assertThat(context.candidateResult().getUpdatedBy()).isEqualTo(teacherId);
        assertThat(context.assignment().getStatus()).isEqualTo(GradingAssignmentStatus.COMPLETED);
        verify(examCandidateResultRepository).save(context.candidateResult());
        verify(examGradingAssignmentRepository).save(context.assignment());
    }

    @Test
    void should_reject_invalidating_a_session_that_was_never_flagged() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, false);

        // Điểm kém là việc của /grade. Đường này chỉ để kết luận vi phạm.
        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã bị đánh dấu nghi vấn");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_reject_invalidating_a_result_that_is_already_released() {
        given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.RELEASED, true);

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("không còn ở trạng thái chờ chấm");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_reject_when_assignment_is_already_completed() {
        given(GradingAssignmentStatus.COMPLETED, ExamCandidateResultStatus.PENDING_REVIEW, true);

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chốt");

        verify(examCandidateResultRepository, never()).save(any());
    }

    @Test
    void should_reject_when_teacher_is_not_the_assigned_one() {
        var context = given(GradingAssignmentStatus.ASSIGNED, ExamCandidateResultStatus.PENDING_REVIEW, true);
        org.mockito.Mockito.doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examGradingAccessService).authorizeGrader(eq(context), eq(teacherId));

        assertThatThrownBy(() -> useCase.execute(command()))
            .isInstanceOf(ForbiddenException.class);

        verify(examCandidateResultRepository, never()).save(any());
    }
}
