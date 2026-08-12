package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsForExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionStatusCountsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.UserDto;

import graphql.schema.DataFetchingEnvironment;

class QuestionControllerTests {

    private QuestionController controller;

    private final UUID questionId = UUID.randomUUID();
    private final UUID createdById = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new QuestionController(
            mock(ViewQuestionsUseCase.class),
            mock(ViewQuestionsForExamPaperUseCase.class),
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewQuestionStatusCountsUseCase.class),
            mock(UserContextPort.class));
    }

    private QuestionDto question(UUID createdBy) {
        return new QuestionDto(questionId, UUID.randomUUID(), UUID.randomUUID(), "Q1", null,
            "Nội dung câu hỏi", null, null, "SPEAKING", 30, 30, 90, "PRIVATE", null, false,
            "DRAFT", "NORMAL", null, null, null, createdBy, null);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_resolve_created_by_user_via_user_data_loader() {
        var dto = question(createdById);
        var user = new UserDto(createdById, "teacher@example.com", null, "Nguyễn Văn A",
            null, null, null, null, null, null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userById")).thenReturn(loader);
        when(loader.load(createdById)).thenReturn(CompletableFuture.completedFuture(user));

        var result = controller.createdByUser(dto, env);

        assertThat(result.join()).isSameAs(user);
        verify(loader).load(createdById);
    }

    @Test
    void should_return_null_created_by_user_when_created_by_is_null() {
        var dto = question(null);
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        var result = controller.createdByUser(dto, env);

        assertThat(result.join()).isNull();
        verify(env, never()).getDataLoader(any());
    }
}
