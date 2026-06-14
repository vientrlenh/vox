package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

class CreateQuestionAssetsUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionAssetRepository assetRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private CreateQuestionAssetsUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        assetRepository = mock(QuestionAssetRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateQuestionAssetsUseCase(questionRepository, assetRepository, permissionQuery, userContextPort);
    }

    @Test
    void create_should_save_assets_and_touch_question() {
        var userId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var question = question(questionId, QuestionStatus.DRAFT);
        var command = command(questionId);
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(assetRepository.findByQuestionId(questionId)).thenReturn(List.of());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(command);

        assertThat(response.questionId()).isEqualTo(questionId);
        verify(assetRepository).saveAll(any());
        assertThat(question.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void create_should_throw_when_assets_already_exist() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(assetRepository.findByQuestionId(questionId)).thenReturn(List.of(
            new QuestionAsset(questionId, "Existing", null, null, QuestionAssetType.IMAGE, "url", null, null, 1)
        ));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command(questionId)));
    }

    private UpdateQuestionAssetsCommand command(UUID questionId) {
        return new UpdateQuestionAssetsCommand(questionId, List.of(
            new UpdateQuestionAssetsCommand.AssetItem("Image", null, "Alt", "IMAGE", "url", null, "desc", 1)
        ));
    }

    private Question question(UUID id, QuestionStatus status) {
        return new Question(id, UUID.randomUUID(), "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
