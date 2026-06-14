package com.sep.vox.application.usecase.questiontopic;

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
import com.sep.vox.application.port.input.command.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.DeleteQuestionTopicUseCase;
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

class DeleteQuestionTopicUseCaseTests {

    private QuestionTopicRepository questionTopicRepository;
    private QuestionRepository questionRepository;
    private QuestionAssetRepository assetRepository;
    private QuestionEvaluationGuideRepository guideRepository;
    private QuestionTopicPermissionQuery permissionQuery;
    private DeleteQuestionTopicUseCase useCase;

    @BeforeEach
    void setUp() {
        questionTopicRepository = mock(QuestionTopicRepository.class);
        questionRepository = mock(QuestionRepository.class);
        assetRepository = mock(QuestionAssetRepository.class);
        guideRepository = mock(QuestionEvaluationGuideRepository.class);
        permissionQuery = mock(QuestionTopicPermissionQuery.class);
        useCase = new DeleteQuestionTopicUseCase(questionTopicRepository, questionRepository, assetRepository, guideRepository, permissionQuery);
    }

    @Test
    void delete_should_remove_questions_assets_and_guides_for_draft_topic() {
        var topicId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        when(permissionQuery.canUpdateTopic(topicId)).thenReturn(true);
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic(topicId, QuestionTopicStatus.DRAFT)));
        when(questionRepository.findByTopicId(topicId)).thenReturn(List.of(question(questionId, topicId)));

        var response = useCase.execute(new DeleteQuestionTopicCommand(topicId));

        assertThat(response.questionTopicId()).isEqualTo(topicId);
        verify(assetRepository).deleteByQuestionId(questionId);
        verify(guideRepository).deleteByQuestionId(questionId);
        verify(questionRepository).deleteById(questionId);
        verify(questionTopicRepository).deleteById(topicId);
    }

    @Test
    void delete_should_throw_when_topic_not_draft() {
        var topicId = UUID.randomUUID();
        when(permissionQuery.canUpdateTopic(topicId)).thenReturn(true);
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic(topicId, QuestionTopicStatus.PUBLISHED)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(new DeleteQuestionTopicCommand(topicId)));
    }

    private QuestionTopic topic(UUID id, QuestionTopicStatus status) {
        return new QuestionTopic(id, UUID.randomUUID(), "TOPIC", "Topic", null, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }

    private Question question(UUID id, UUID topicId) {
        return new Question(id, topicId, "Q1", "Instruction", "Text", "Prompt", "Prep", QuestionType.SHORT_ANSWER,
            10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE, null, false, QuestionStatus.DRAFT,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
