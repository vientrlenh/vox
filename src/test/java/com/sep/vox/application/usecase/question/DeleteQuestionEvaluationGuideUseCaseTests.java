package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

class DeleteQuestionEvaluationGuideUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionEvaluationGuideRepository guideRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private DeleteQuestionEvaluationGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteQuestionEvaluationGuideUseCase(questionRepository, guideRepository, permissionQuery, userContextPort);
    }

    @Test
    void delete_should_remove_guide_for_draft_question() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.of(new QuestionEvaluationGuide()));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(questionId);

        assertThat(response.questionId()).isEqualTo(questionId);
        verify(guideRepository).deleteByQuestionId(questionId);
    }

    @Test
    void delete_should_throw_when_question_not_draft() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.PUBLISHED)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(questionId));
    }

    @Test
    void delete_should_throw_when_guide_missing() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(questionId));
    }

    private Question question(UUID id, QuestionStatus status) {
        return new Question(id, UUID.randomUUID(), "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
