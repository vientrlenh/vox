package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.assessmentgrading.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyGradingAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;

/**
 * The teacher's queue and its assessment filter dropdown -- two places that must see the SAME set of attempts.
 *
 * <p>What needs locking is the scope: teacherId is always taken from the token, {@code assessmentKind} is always locked to
 * centralized assessments, and {@code assessmentId} is accepted from the client but only comes along and does not replace
 * the other two. If any one of the three is off, the filter turns into a door.
 */
class ViewMyGradingQueueScopeTests {

    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ViewMyGradingTasksUseCase tasksUseCase;
    private ViewMyGradingAssessmentsUseCase assessmentsUseCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        tasksUseCase = new ViewMyGradingTasksUseCase(
            assessmentGradingQueryRepository, assessmentGradingAccessService);
        assessmentsUseCase = new ViewMyGradingAssessmentsUseCase(
            assessmentGradingQueryRepository, assessmentGradingAccessService);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(assessmentGradingQueryRepository.findTasksByTeacherIdAndAssessmentId(
            any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
        when(assessmentGradingQueryRepository.findAssessmentsWithTasksByTeacherId(any(), anyString()))
            .thenReturn(List.of());
    }

    @Test
    void should_pass_the_assessment_id_filter_through_to_the_query_repository() {
        tasksUseCase.execute(new ViewMyGradingTasksQuery(assessmentId, null, null, 0, 20));

        ArgumentCaptor<UUID> captured = ArgumentCaptor.forClass(UUID.class);
        verify(assessmentGradingQueryRepository).findTasksByTeacherIdAndAssessmentId(
            any(), captured.capture(), any(), any(), any(), anyInt(), anyInt());
        assertThat(captured.getValue()).isEqualTo(assessmentId);
    }

    /** An empty assessmentId = every assessment, exactly the behavior from before the filter existed. */
    @Test
    void should_not_narrow_by_assessment_when_no_assessment_id_is_given() {
        tasksUseCase.execute(new ViewMyGradingTasksQuery(null, "ASSIGNED", null, 0, 20));

        ArgumentCaptor<UUID> captured = ArgumentCaptor.forClass(UUID.class);
        verify(assessmentGradingQueryRepository).findTasksByTeacherIdAndAssessmentId(
            any(), captured.capture(), any(), any(), any(), anyInt(), anyInt());
        assertThat(captured.getValue()).isNull();
    }

    /**
     * A class test has its own screen, where the teacher also sees the student's name. Accepting an
     * {@code assessmentId} and forgetting to lock {@code kind} would let the id of a class test into the
     * anonymous grading queue.
     */
    @Test
    void should_lock_the_teacher_queue_to_centralized_assessments_even_with_an_assessment_id() {
        tasksUseCase.execute(new ViewMyGradingTasksQuery(assessmentId, null, null, 0, 20));

        ArgumentCaptor<UUID> teacher = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> kind = ArgumentCaptor.forClass(String.class);
        verify(assessmentGradingQueryRepository).findTasksByTeacherIdAndAssessmentId(
            teacher.capture(), any(), kind.capture(), any(), any(), anyInt(), anyInt());
        assertThat(teacher.getValue()).isEqualTo(teacherId);
        assertThat(kind.getValue()).isEqualTo("CENTRALIZED");
    }

    /** The dropdown and the table must filter on the same set, otherwise it lists an empty assessment. */
    @Test
    void should_scope_the_assessment_picker_to_the_caller_and_to_centralized_assessments() {
        assessmentsUseCase.execute(null);

        verify(assessmentGradingQueryRepository).findAssessmentsWithTasksByTeacherId(teacherId, "CENTRALIZED");
    }
}
