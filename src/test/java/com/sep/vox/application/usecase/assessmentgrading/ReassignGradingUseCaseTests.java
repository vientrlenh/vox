package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.command.assessmentgrading.ReassignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReassignGradingUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Changing the grader is a back door around the conflict-of-interest rule: assigning an appeal through the right door is
 * blocked, but changing the person through here used to slip through (review BE-2).
 */
class ReassignGradingUseCaseTests {

    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ReassignGradingUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID newTeacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new ReassignGradingUseCase(
            assessmentGradingAssignmentRepository, assessmentGradingQueryRepository, assessmentGradingAccessService,
            mock(GradingAssignmentNotificationService.class));

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAccessService.isTeacherOfSchool(newTeacherId, schoolId)).thenReturn(true);
        when(assessmentGradingAssignmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of());
    }

    private AssessmentGradingAssignment given(GradingRoundType roundType) {
        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), roundType, null, null,
            Instant.now().minus(2, ChronoUnit.DAYS), adminId, Instant.now().plus(1, ChronoUnit.DAYS));
        assignment.setId(UUID.randomUUID());
        assignment.setRemindedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        AssessmentCandidateResult candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(candidateResultId);
        when(assessmentGradingAccessService.loadForUpdate(assignment.getId())).thenReturn(new GradingContext(
            assignment, candidateResult, new AssessmentSession(), schoolId, "IELTS Mock", AssessmentKind.CENTRALIZED));
        return assignment;
    }

    @Test
    void should_reject_an_appeal_reviewer_who_already_graded_this_paper() {
        AssessmentGradingAssignment assignment = given(GradingRoundType.APPEAL);
        when(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(newTeacherId));

        assertThatThrownBy(() -> useCase.execute(
            new ReassignGradingCommand(assignment.getId(), newTeacherId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("appeal");

        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_allow_a_reviewer_who_never_graded_this_paper() {
        AssessmentGradingAssignment assignment = given(GradingRoundType.APPEAL);

        useCase.execute(new ReassignGradingCommand(assignment.getId(), newTeacherId));

        assertThat(assignment.getTeacherId()).isEqualTo(newTeacherId);
    }

    @Test
    void should_not_apply_the_conflict_rule_to_the_other_three_rounds() {
        AssessmentGradingAssignment assignment = given(GradingRoundType.SPOT_CHECK);
        when(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(newTeacherId));

        // A post-check does NOT forbid someone who already graded: the COI rule exists only for the
        // appeal round.
        useCase.execute(new ReassignGradingCommand(assignment.getId(), newTeacherId));

        assertThat(assignment.getTeacherId()).isEqualTo(newTeacherId);
    }

    @Test
    void should_clear_the_reminder_mark_so_the_new_teacher_gets_reminded() {
        AssessmentGradingAssignment assignment = given(GradingRoundType.INITIAL);

        useCase.execute(new ReassignGradingCommand(assignment.getId(), newTeacherId));

        // findDueForReminder filters reminded_at IS NULL; keeping the old stamp means the new
        // person never receives the deadline reminder email.
        assertThat(assignment.getRemindedAt()).isNull();
    }
}
