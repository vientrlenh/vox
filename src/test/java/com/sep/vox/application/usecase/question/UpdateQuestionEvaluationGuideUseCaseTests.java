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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionEvaluationGuideUseCase;
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

class UpdateQuestionEvaluationGuideUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionEvaluationGuideRepository guideRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private UpdateQuestionEvaluationGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateQuestionEvaluationGuideUseCase(questionRepository, guideRepository, permissionQuery, userContextPort);
    }

    @Test
    void update_should_modify_existing_guide() {
        var questionId = UUID.randomUUID();
        var existingGuide = new QuestionEvaluationGuide(
            UUID.randomUUID(),
            questionId,
            "Old expected",
            "Old key",
            "Old acceptable",
            "Old off topic",
            "Old hints",
            "Old mistakes"
        );
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.of(existingGuide));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(command(questionId));

        assertThat(response.questionId()).isEqualTo(questionId);
        assertThat(existingGuide.getExpectedContent()).isEqualTo("Expected");
        assertThat(existingGuide.getKeyPoints()).isEqualTo("Key");
        assertThat(existingGuide.getAcceptableResponses()).isEqualTo("Accept");
        assertThat(existingGuide.getOffTopicExamples()).isEqualTo("Off");
        assertThat(existingGuide.getScoringHints()).isEqualTo("Hints");
        assertThat(existingGuide.getCommonMistakes()).isEqualTo("Mistakes");
        verify(guideRepository).save(existingGuide);
    }

    @Test
    void update_should_throw_when_guide_missing() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(guideRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command(questionId)));
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
