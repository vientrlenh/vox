package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.UpdateQuestionContentCommand;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionContentUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionRepository;

class UpdateQuestionContentUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private UpdateQuestionContentUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateQuestionContentUseCase(questionRepository, permissionQuery, userContextPort);
    }

    @Test
    void update_should_save_new_question_content() {
        var userId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var question = question(questionId);
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        var response = useCase.execute(new UpdateQuestionContentCommand(
            questionId, "Instruction", "Updated", "Prompt", "Preparation", "LONG_ANSWER", 15, 30, 60
        ));

        assertThat(response.questionId()).isEqualTo(questionId);
        assertThat(question.getQuestionText()).isEqualTo("Updated");
        assertThat(question.getType()).isEqualTo(QuestionType.LONG_ANSWER);
        assertThat(question.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void update_should_throw_when_permission_denied() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new UpdateQuestionContentCommand(
            questionId, null, "Text", null, null, "SHORT_ANSWER", 10, 20, 30
        )));
    }

    private Question question(UUID id) {
        return new Question(id, UUID.randomUUID(), "Q1", "Instruction", "Old question", "Old prompt", "Old preparation", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, QuestionStatus.DRAFT,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
