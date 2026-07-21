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

import com.sep.vox.application.port.input.query.ViewExamCandidatesQuery;
import com.sep.vox.application.port.input.usecase.examcandidate.ViewExamCandidatesUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;

import graphql.schema.DataFetchingEnvironment;

class ExamCandidateControllerTests {

    private ViewExamCandidatesUseCase viewExamCandidatesUseCase;
    private ExamCandidateController controller;

    private final UUID examId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewExamCandidatesUseCase = mock(ViewExamCandidatesUseCase.class);
        controller = new ExamCandidateController(viewExamCandidatesUseCase);
    }

    @Test
    void should_delegate_query_to_use_case() {
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, null, scheduleId,
            "ASSIGNED", null, null);
        when(viewExamCandidatesUseCase.execute(any(ViewExamCandidatesQuery.class))).thenReturn(List.of(dto));

        var result = controller.examCandidates(examId, scheduleId, ExamCandidateStatus.ASSIGNED);

        assertThat(result).containsExactly(dto);
        verify(viewExamCandidatesUseCase).execute(
            new ViewExamCandidatesQuery(examId, scheduleId, ExamCandidateStatus.ASSIGNED));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_student_via_user_data_loader() {
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, null, scheduleId,
            "ASSIGNED", null, null);
        var user = new UserDto(studentId, "student@example.com", null, "Student",
            null, null, null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userById")).thenReturn(loader);
        when(loader.load(studentId)).thenReturn(CompletableFuture.completedFuture(user));

        var result = controller.student(dto, env);

        assertThat(result.join()).isSameAs(user);
        verify(loader).load(studentId);
    }

    @Test
    void should_return_null_assigned_paper_when_not_assigned() {
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, null, scheduleId,
            "ASSIGNED", null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        var result = controller.assignedPaper(dto, env);

        assertThat(result.join()).isNull();
        verify(env, org.mockito.Mockito.never()).getDataLoader(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_assigned_paper_via_data_loader() {
        var paperId = UUID.randomUUID();
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, paperId, scheduleId,
            "ASSIGNED", null, null);
        var paper = new ExamPaperDto(paperId, examId, null, "P1", 1, "LOCKED", null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, ExamPaperDto>getDataLoader("examPaperById")).thenReturn(loader);
        when(loader.load(paperId)).thenReturn(CompletableFuture.completedFuture(paper));

        var result = controller.assignedPaper(dto, env);

        assertThat(result.join()).isSameAs(paper);
        verify(loader).load(paperId);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_schedule_via_data_loader() {
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, null, scheduleId,
            "ASSIGNED", null, null);
        var schedule = new ExamScheduleDto(scheduleId, examId, UUID.randomUUID(), null, null, "DRAFT", null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, ExamScheduleDto>getDataLoader("examScheduleById")).thenReturn(loader);
        when(loader.load(scheduleId)).thenReturn(CompletableFuture.completedFuture(schedule));

        var result = controller.schedule(dto, env);

        assertThat(result.join()).isSameAs(schedule);
        verify(loader).load(scheduleId);
    }

    @Test
    void should_return_null_schedule_when_not_assigned() {
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, null, null,
            "ASSIGNED", null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        var result = controller.schedule(dto, env);

        assertThat(result.join()).isNull();
        verify(env, org.mockito.Mockito.never()).getDataLoader(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_exam_via_data_loader() {
        var dto = new ExamCandidateDto(UUID.randomUUID(), examId, studentId, null, scheduleId,
            "ASSIGNED", null, null);
        var exam = new ExamDto(examId, null, null, "E1", "Exam", null, UUID.randomUUID(), UUID.randomUUID(),
            "CENTRALIZED", "LAB", "DRAFT", null, null, null, null, null, false, null, null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, ExamDto>getDataLoader("examById")).thenReturn(loader);
        when(loader.load(examId)).thenReturn(CompletableFuture.completedFuture(exam));

        var result = controller.exam(dto, env);

        assertThat(result.join()).isSameAs(exam);
        verify(loader).load(examId);
    }
}
