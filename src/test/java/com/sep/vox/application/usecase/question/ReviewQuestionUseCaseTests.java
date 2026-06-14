package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.usecase.question.ReviewQuestionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionRepository;

class ReviewQuestionUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private ReviewQuestionUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ReviewQuestionUseCase(questionRepository, permissionQuery, userContextPort);
    }

    @Test
    void review_should_update_question_status() {
        var userId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var question = question(questionId);
        when(permissionQuery.canReview(questionId, QuestionStatus.APPROVED)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        var response = useCase.execute(new ReviewQuestionCommand(questionId, QuestionStatus.APPROVED, "ok", "ready"));

        assertThat(response.questionId()).isEqualTo(questionId);
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.APPROVED);
        assertThat(question.getUpdatedBy()).isEqualTo(userId);
        verify(permissionQuery).canReview(questionId, QuestionStatus.APPROVED);
    }

    @Test
    void review_should_throw_when_permission_denied() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canReview(questionId, QuestionStatus.REJECTED)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ReviewQuestionCommand(questionId, QuestionStatus.REJECTED, null, null)));
        verify(permissionQuery).canReview(questionId, QuestionStatus.REJECTED);
        verifyNoInteractions(questionRepository, userContextPort);
    }

    private Question question(UUID id) {
        return new Question(id, UUID.randomUUID(), "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, QuestionStatus.SUBMITTED_FOR_REVIEW,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
