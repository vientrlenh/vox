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
}
