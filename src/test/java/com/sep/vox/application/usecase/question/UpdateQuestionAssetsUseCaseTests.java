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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetsUseCase;
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

class UpdateQuestionAssetsUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionAssetRepository assetRepository;
    private QuestionPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private UpdateQuestionAssetsUseCase useCase;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        assetRepository = mock(QuestionAssetRepository.class);
        permissionQuery = mock(QuestionPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateQuestionAssetsUseCase(questionRepository, assetRepository, permissionQuery, userContextPort);
    }

    @Test
    void update_should_replace_assets_and_touch_question() {
        var userId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(assetRepository.findByQuestionId(questionId)).thenReturn(List.of(
            new QuestionAsset(questionId, "Old", null, null, QuestionAssetType.IMAGE, "old", null, null, 1)
        ));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(command(questionId));

        assertThat(response.questionId()).isEqualTo(questionId);
        verify(assetRepository).deleteByQuestionId(questionId);
        verify(assetRepository).saveAll(any());
    }

    @Test
    void update_should_throw_when_no_existing_assets() {
        var questionId = UUID.randomUUID();
        when(permissionQuery.canEditContent(questionId)).thenReturn(true);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question(questionId, QuestionStatus.DRAFT)));
        when(assetRepository.findByQuestionId(questionId)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> useCase.execute(command(questionId)));
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
