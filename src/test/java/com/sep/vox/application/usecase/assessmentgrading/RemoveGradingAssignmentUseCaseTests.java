package com.sep.vox.application.usecase.assessmentgrading;

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
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * Gỡ phân công xoá hẳn dòng, tức xoá luôn con trỏ tới đơn phúc khảo — nên đơn phải
 * được nhả TRƯỚC khi xoá, nếu không nó kẹt ở {@code GRADING} không lối ra (review BE-1).
 */
class RemoveGradingAssignmentUseCaseTests {

    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private RemoveGradingAssignmentUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();

    private AssessmentResultAppeal appeal;

    @BeforeEach
    void setUp() {
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new RemoveGradingAssignmentUseCase(
            assessmentGradingAssignmentRepository, assessmentResultAppealRepository, assessmentGradingAccessService);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);

        appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(AssessmentAppealStatus.GRADING);
        when(assessmentResultAppealRepository.findById(appealId)).thenReturn(Optional.of(appeal));
    }

    private UUID given(AssessmentGradingAssignment assignment) {
        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(candidateResultId);
        when(assessmentGradingAccessService.load(assignment.getId())).thenReturn(new GradingContext(
            assignment, candidateResult, new AssessmentSession(), schoolId, "IELTS Mock", AssessmentKind.CENTRALIZED));
        return assignment.getId();
    }

    private AssessmentGradingAssignment assignment(GradingRoundType roundType, UUID linkedAppealId) {
        AssessmentGradingAssignment open = AssessmentGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), roundType, linkedAppealId, null,
            Instant.now(), adminId, null);
        open.setId(UUID.randomUUID());
        return open;
    }

    @Test
    void should_release_the_appeal_before_deleting_the_assignment() {
        UUID assignmentId = given(assignment(GradingRoundType.APPEAL, appealId));

        useCase.execute(new RemoveGradingAssignmentCommand(assignmentId));

        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.APPROVED);
        verify(assessmentResultAppealRepository).save(appeal);
        verify(assessmentGradingAssignmentRepository).deleteById(assignmentId);
    }

    @Test
    void should_leave_appeals_alone_for_the_other_three_rounds() {
        UUID assignmentId = given(assignment(GradingRoundType.SPOT_CHECK, null));

        useCase.execute(new RemoveGradingAssignmentCommand(assignmentId));

        verify(assessmentResultAppealRepository, never()).findById(any());
        verify(assessmentGradingAssignmentRepository).deleteById(assignmentId);
    }

    @Test
    void should_not_touch_an_appeal_that_is_no_longer_being_graded() {
        appeal.setStatus(AssessmentAppealStatus.PUBLISHED);
        UUID assignmentId = given(assignment(GradingRoundType.APPEAL, appealId));

        useCase.execute(new RemoveGradingAssignmentCommand(assignmentId));

        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.PUBLISHED);
        verify(assessmentResultAppealRepository, never()).save(any());
    }

    @Test
    void should_refuse_removing_an_assignment_that_is_already_completed() {
        AssessmentGradingAssignment completed = assignment(GradingRoundType.APPEAL, appealId);
        completed.complete(GradingOutcome.UPHELD, null, Instant.now());
        UUID assignmentId = given(completed);

        assertThatThrownBy(() -> useCase.execute(new RemoveGradingAssignmentCommand(assignmentId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã chấm xong");

        verify(assessmentResultAppealRepository, never()).save(any());
        verify(assessmentGradingAssignmentRepository, never()).deleteById(any());
    }
}
