package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentgrading.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewClassTestGradingResultsUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * The list of EVERY submission of a class test -- including submissions that have no assignment yet.
 *
 * <p>This is the only place that emits the {@code candidateResultId} of cleanly AI-graded submissions
 * (they go straight to RELEASED so no assignment is opened automatically). Without it, a student's second
 * attempt has no way into the grading screen.
 */
class ViewClassTestGradingResultsUseCaseTests {

    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ViewClassTestGradingResultsUseCase useCase;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new ViewClassTestGradingResultsUseCase(
            assessmentGradingQueryRepository, assessmentGradingAccessService);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(assessmentGradingAccessService.requireCurrentSchoolId(teacherId)).thenReturn(schoolId);
        when(assessmentGradingQueryRepository.searchAssignments(any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    }

    @Test
    void should_scope_the_query_to_the_class_test_of_the_caller() {
        useCase.execute(new ViewClassTestGradingResultsQuery(assessmentId, null, false, null, 0, 20));

        ArgumentCaptor<GradingAssignmentFilter> filter = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(assessmentGradingQueryRepository).searchAssignments(filter.capture(), anyInt(), anyInt());
        assertThat(filter.getValue().schoolId()).isEqualTo(schoolId);
        assertThat(filter.getValue().assessmentId()).isEqualTo(assessmentId);
        assertThat(filter.getValue().assessmentKind()).isEqualTo("CLASS_TEST");
    }

    /**
     * Do not filter by assignment: submissions NOT yet assigned to anyone are exactly what the
     * teacher needs to see here.
     */
    @Test
    void should_not_narrow_the_list_to_assigned_results() {
        useCase.execute(new ViewClassTestGradingResultsQuery(assessmentId, null, false, null, 0, 20));

        ArgumentCaptor<GradingAssignmentFilter> filter = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(assessmentGradingQueryRepository).searchAssignments(filter.capture(), anyInt(), anyInt());
        assertThat(filter.getValue().teacherId()).isNull();
        assertThat(filter.getValue().assignmentStatus()).isNull();
        assertThat(filter.getValue().unassignedOnly()).isFalse();
    }

    @Test
    void should_pass_the_unassigned_only_filter_through() {
        useCase.execute(new ViewClassTestGradingResultsQuery(
            assessmentId, AssessmentCandidateResultStatus.RELEASED, true, "thiên", 0, 20));

        ArgumentCaptor<GradingAssignmentFilter> filter = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(assessmentGradingQueryRepository).searchAssignments(filter.capture(), anyInt(), anyInt());
        assertThat(filter.getValue().unassignedOnly()).isTrue();
        assertThat(filter.getValue().resultStatus()).isEqualTo("RELEASED");
        assertThat(filter.getValue().keyword()).isEqualTo("thiên");
    }

    /**
     * The permission closes to exactly the tests the caller is CHAIR of. Without this case, any teacher
     * could read the student list with scores of every test in the school.
     */
    @Test
    void should_reject_a_teacher_who_is_not_the_chair_of_the_class_test() {
        doThrow(new ForbiddenException("Access denied"))
            .when(assessmentGradingAccessService).authorizeClassTestChair(assessmentId, teacherId);

        assertThatThrownBy(() -> useCase.execute(
            new ViewClassTestGradingResultsQuery(assessmentId, null, false, null, 0, 20)))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentGradingQueryRepository, never()).searchAssignments(any(), anyInt(), anyInt());
    }
}
