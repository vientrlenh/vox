package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.RemoveGradingAssignmentCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.examgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Gỡ phân công xoá hẳn dòng, tức xoá luôn con trỏ tới đơn phúc khảo — nên đơn phải
 * được nhả TRƯỚC khi xoá, nếu không nó kẹt ở {@code GRADING} không lối ra (review BE-1).
 */
class RemoveGradingAssignmentUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private ExamGradingAccessService examGradingAccessService;
    private RemoveGradingAssignmentUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();

    private ExamResultAppeal appeal;

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new RemoveGradingAssignmentUseCase(
            examGradingAssignmentRepository, examResultAppealRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);

        appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(ExamAppealStatus.GRADING);
        when(examResultAppealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
    }

    private UUID given(ExamGradingAssignment assignment) {
        var candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        when(examGradingAccessService.load(assignment.getId())).thenReturn(new GradingContext(
            assignment, candidateResult, new ExamSession(), schoolId, "IELTS Mock"));
        return assignment.getId();
    }

    private ExamGradingAssignment assignment(GradingRoundType roundType, UUID linkedAppealId) {
        var open = ExamGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), roundType, linkedAppealId, null,
            Instant.now(), adminId, null);
        open.setId(UUID.randomUUID());
        return open;
    }

    @Test
    void should_release_the_appeal_before_deleting_the_assignment() {
        var assignmentId = given(assignment(GradingRoundType.APPEAL, appealId));

        useCase.execute(new RemoveGradingAssignmentCommand(assignmentId));

        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.APPROVED);
        verify(examResultAppealRepository).save(appeal);
        verify(examGradingAssignmentRepository).deleteById(assignmentId);
    }

    @Test
    void should_leave_appeals_alone_for_the_other_three_rounds() {
        var assignmentId = given(assignment(GradingRoundType.SPOT_CHECK, null));

        useCase.execute(new RemoveGradingAssignmentCommand(assignmentId));

        verify(examResultAppealRepository, never()).findById(any());
        verify(examGradingAssignmentRepository).deleteById(assignmentId);
    }

    @Test
    void should_not_touch_an_appeal_that_is_no_longer_being_graded() {
        appeal.setStatus(ExamAppealStatus.PUBLISHED);
        var assignmentId = given(assignment(GradingRoundType.APPEAL, appealId));

        useCase.execute(new RemoveGradingAssignmentCommand(assignmentId));

        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.PUBLISHED);
        verify(examResultAppealRepository, never()).save(any());
    }

    @Test
    void should_refuse_removing_an_assignment_that_is_already_completed() {
        var completed = assignment(GradingRoundType.APPEAL, appealId);
        completed.complete(GradingOutcome.UPHELD, null, Instant.now());
        var assignmentId = given(completed);

        assertThatThrownBy(() -> useCase.execute(new RemoveGradingAssignmentCommand(assignmentId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chấm xong");

        verify(examResultAppealRepository, never()).save(any());
        verify(examGradingAssignmentRepository, never()).deleteById(any());
    }
}
