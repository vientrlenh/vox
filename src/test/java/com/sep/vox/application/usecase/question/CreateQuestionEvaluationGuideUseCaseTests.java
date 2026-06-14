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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionEvaluationGuideUseCase;
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

class CreateQuestionEvaluationGuideUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionEvaluationGuideRepository guideRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private CreateQuestionEvaluationGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateQuestionEvaluationGuideUseCase(questionRepository, guideRepository, permissionQuery, userContextPort);
    }

    @Test
    void create_should_save_guide_and_touch_question() {
        var userId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(command(questionId));

        assertThat(response.questionId()).isEqualTo(questionId);
        verify(guideRepository).save(any(QuestionEvaluationGuide.class));
    }

    @Test
    void create_should_throw_when_guide_exists() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.of(new QuestionEvaluationGuide()));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command(questionId)));
    }

    private UpdateQuestionEvaluationGuideCommand command(UUID questionId) {
        return new UpdateQuestionEvaluationGuideCommand(questionId, "Expected", "Key", "Accept", "Off", "Hints", "Mistakes");
    }

    private Question question(UUID id, QuestionStatus status) {
        return new Question(id, UUID.randomUUID(), "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
