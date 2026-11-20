package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.UpdateAssessmentCandidateStatusCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentCandidatesAttendanceCommand;
import com.sep.vox.application.port.input.query.ViewAssessmentCandidatesQuery;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidateStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidatesAttendanceUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewAssessmentCandidatesUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;

import graphql.schema.DataFetchingEnvironment;

class AssessmentCandidateControllerTests {

    private ViewAssessmentCandidatesUseCase viewAssessmentCandidatesUseCase;
    private UpdateAssessmentCandidateStatusUseCase updateAssessmentCandidateStatusUseCase;
    private UpdateAssessmentCandidatesAttendanceUseCase updateAssessmentCandidatesAttendanceUseCase;
    private AssessmentCandidateController controller;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewAssessmentCandidatesUseCase = mock(ViewAssessmentCandidatesUseCase.class);
        updateAssessmentCandidateStatusUseCase = mock(UpdateAssessmentCandidateStatusUseCase.class);
        updateAssessmentCandidatesAttendanceUseCase = mock(UpdateAssessmentCandidatesAttendanceUseCase.class);
        controller = new AssessmentCandidateController(viewAssessmentCandidatesUseCase, updateAssessmentCandidateStatusUseCase,
            updateAssessmentCandidatesAttendanceUseCase);
    }

    private AssessmentCandidateDto candidate(UUID assignedPaperId, UUID scheduleId) {
        return new AssessmentCandidateDto(UUID.randomUUID(), assessmentId, studentId, assignedPaperId, scheduleId,
            "ASSIGNED", null, null, null);
    }

    @Test
    void should_delegate_query_to_use_case() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        when(viewAssessmentCandidatesUseCase.execute(any(ViewAssessmentCandidatesQuery.class))).thenReturn(List.of(dto));

        List<AssessmentCandidateDto> result = controller.assessmentCandidates(assessmentId, scheduleId, AssessmentCandidateStatus.ASSIGNED);

        assertThat(result).containsExactly(dto);
        verify(viewAssessmentCandidatesUseCase).execute(
            new ViewAssessmentCandidatesQuery(assessmentId, scheduleId, AssessmentCandidateStatus.ASSIGNED));
    }

    @Test
    void should_return_candidate_id_when_updating_status() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        when(updateAssessmentCandidateStatusUseCase.execute(any(UpdateAssessmentCandidateStatusCommand.class)))
            .thenReturn(dto);

        UUID result = controller.updateAssessmentCandidateStatus(dto.id(), AssessmentCandidateStatus.ABSENT);

        assertThat(result).isEqualTo(dto.id());
        verify(updateAssessmentCandidateStatusUseCase).execute(
            new UpdateAssessmentCandidateStatusCommand(dto.id(), AssessmentCandidateStatus.ABSENT));
    }

    @Test
    void should_delegate_attendance_update_to_use_case() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        List<UUID> candidateIds = List.of(dto.id());
        when(updateAssessmentCandidatesAttendanceUseCase.execute(any(UpdateAssessmentCandidatesAttendanceCommand.class)))
            .thenReturn(List.of(dto));

        List<AssessmentCandidateDto> result = controller.updateAssessmentCandidatesAttendance(scheduleId, candidateIds);

        assertThat(result).containsExactly(dto);
        verify(updateAssessmentCandidatesAttendanceUseCase).execute(
            new UpdateAssessmentCandidatesAttendanceCommand(scheduleId, candidateIds));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_student_via_user_data_loader() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        UserDto user = new UserDto(studentId, "student@example.com", null, "Student",
            null, null, null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userById")).thenReturn(loader);
        when(loader.load(studentId)).thenReturn(CompletableFuture.completedFuture(user));

        CompletableFuture<UserDto> result = controller.student(dto, env);

        assertThat(result.join()).isSameAs(user);
        verify(loader).load(studentId);
    }

    @Test
    void should_return_null_assigned_paper_when_not_assigned() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        CompletableFuture<AssessmentPaperDto> result = controller.assignedPaper(dto, env);

        assertThat(result.join()).isNull();
        verify(env, org.mockito.Mockito.never()).getDataLoader(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_assigned_paper_via_data_loader() {
        UUID paperId = UUID.randomUUID();
        AssessmentCandidateDto dto = candidate(paperId, scheduleId);
        AssessmentPaperDto paper = new AssessmentPaperDto(paperId, assessmentId, null, "P1", 1, "LOCKED", null, null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, AssessmentPaperDto>getDataLoader("assessmentPaperById")).thenReturn(loader);
        when(loader.load(paperId)).thenReturn(CompletableFuture.completedFuture(paper));

        CompletableFuture<AssessmentPaperDto> result = controller.assignedPaper(dto, env);

        assertThat(result.join()).isSameAs(paper);
        verify(loader).load(paperId);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_schedule_via_data_loader() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        AssessmentScheduleDto schedule = new AssessmentScheduleDto(scheduleId, assessmentId, UUID.randomUUID(), null, null, "DRAFT", null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, AssessmentScheduleDto>getDataLoader("assessmentScheduleById")).thenReturn(loader);
        when(loader.load(scheduleId)).thenReturn(CompletableFuture.completedFuture(schedule));

        CompletableFuture<AssessmentScheduleDto> result = controller.schedule(dto, env);

        assertThat(result.join()).isSameAs(schedule);
        verify(loader).load(scheduleId);
    }

    @Test
    void should_return_null_schedule_when_not_assigned() {
        AssessmentCandidateDto dto = candidate(null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        CompletableFuture<AssessmentScheduleDto> result = controller.schedule(dto, env);

        assertThat(result.join()).isNull();
        verify(env, org.mockito.Mockito.never()).getDataLoader(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_assessment_via_data_loader() {
        AssessmentCandidateDto dto = candidate(null, scheduleId);
        AssessmentDto assessment = new AssessmentDto(assessmentId, null, null, "E1", "Assessment", null, UUID.randomUUID(), UUID.randomUUID(),
            "CENTRALIZED", "LAB", "DRAFT", null, null, null, null, null, null, false, null, null, null, null, null,
            null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, AssessmentDto>getDataLoader("assessmentById")).thenReturn(loader);
        when(loader.load(assessmentId)).thenReturn(CompletableFuture.completedFuture(assessment));

        CompletableFuture<AssessmentDto> result = controller.assessment(dto, env);

        assertThat(result.join()).isSameAs(assessment);
        verify(loader).load(assessmentId);
    }
}
