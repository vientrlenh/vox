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
import com.sep.vox.application.port.input.command.DeleteQuestionCommand;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

class DeleteQuestionUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionAssetRepository assetRepository;
    private QuestionEvaluationGuideRepository guideRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private DeleteQuestionUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        assetRepository = mock(QuestionAssetRepository.class);
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteQuestionUseCase(questionRepository, assetRepository, guideRepository, permissionQuery, userContextPort);
    }

    @Test
    void delete_should_hard_delete_when_question_is_draft_and_unused() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(questionRepository.existsBySourceQuestionId(questionId)).thenReturn(false);

        var response = useCase.execute(new DeleteQuestionCommand(questionId));

        assertThat(response.questionId()).isEqualTo(questionId);
        assertThat(response.deleteMode()).isEqualTo("HARD_DELETE");
        verify(assetRepository).deleteByQuestionId(questionId);
        verify(guideRepository).deleteByQuestionId(questionId);
        verify(questionRepository).deleteById(questionId);
    }

    @Test
    void delete_should_archive_when_question_is_used() {
        var userId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var question = question(questionId, QuestionStatus.DRAFT);
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.existsBySourceQuestionId(questionId)).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        var response = useCase.execute(new DeleteQuestionCommand(questionId));

        assertThat(response.deleteMode()).isEqualTo("ARCHIVE");
        assertThat(response.resultingStatus()).isEqualTo("ARCHIVED");
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.ARCHIVED);
        assertThat(question.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void delete_should_throw_when_permission_denied() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new DeleteQuestionCommand(questionId)));
    }

    private Question question(UUID id, QuestionStatus status) {
        return new Question(id, UUID.randomUUID(), "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
