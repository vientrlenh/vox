package com.sep.vox.application.usecase.questionbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankUseCase;
import com.sep.vox.application.query.repository.QuestionBankPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

class DeleteQuestionBankUseCaseTests {

    private QuestionBankRepository questionBankRepository;
    private QuestionTopicRepository questionTopicRepository;
    private QuestionRepository questionRepository;
    private QuestionAssetRepository questionAssetRepository;
    private QuestionEvaluationGuideRepository guideRepository;
    private QuestionBankPermissionQuery permissionQuery;
    private DeleteQuestionBankUseCase useCase;

    @BeforeEach
    void setUp() {
        questionBankRepository = mock(QuestionBankRepository.class);
        questionTopicRepository = mock(QuestionTopicRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionAssetRepository = mock(QuestionAssetRepository.class);
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        permissionQuery = mock(QuestionBankPermissionQuery.class);
        useCase = new DeleteQuestionBankUseCase(
            questionBankRepository, questionTopicRepository, questionRepository, questionAssetRepository, guideRepository, permissionQuery
        );
    }

    @Test
    void delete_should_remove_topics_questions_assets_and_guides_for_draft_bank() {
        var bankId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var questionId = UUID.randomUUID();

        when(permissionQuery.canUpdateBank(bankId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank(bankId, QuestionBankStatus.DRAFT)));
        when(questionTopicRepository.findByQuestionBankId(bankId)).thenReturn(List.of(topic(topicId, bankId)));
        when(questionRepository.findByTopicId(topicId)).thenReturn(List.of(question(questionId, topicId, QuestionStatus.DRAFT)));

        var response = useCase.execute(new DeleteQuestionBankCommand(bankId));

        assertThat(response.questionBankId()).isEqualTo(bankId);
        verify(questionAssetRepository).deleteByQuestionId(questionId);
        verify(guideRepository).deleteByQuestionId(questionId);
        verify(questionRepository).deleteById(questionId);
        verify(questionTopicRepository).deleteById(topicId);
        verify(questionBankRepository).deleteById(bankId);
    }

    @Test
    void delete_should_throw_when_bank_not_draft() {
        var bankId = UUID.randomUUID();
        when(permissionQuery.canUpdateBank(bankId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank(bankId, QuestionBankStatus.PUBLISHED)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(new DeleteQuestionBankCommand(bankId)));
    }

    private QuestionBank bank(UUID id, QuestionBankStatus status) {
        return new QuestionBank(id, UUID.randomUUID(), null, "BANK", "Bank", null, QuestionBankOwnerType.SYSTEM, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }

    private QuestionTopic topic(UUID id, UUID bankId) {
        return new QuestionTopic(id, bankId, "TOPIC", "Topic", null, QuestionTopicStatus.DRAFT,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }

    private Question question(UUID id, UUID topicId, QuestionStatus status) {
        return new Question(id, topicId, "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 40, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
