package com.sep.vox.application.usecase.questiontopic;

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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

class UpdateQuestionTopicUseCaseTests {

    private QuestionTopicRepository questionTopicRepository;
    private QuestionBankRepository questionBankRepository;
    private UserContextPort userContextPort;
    private UpdateQuestionTopicUseCase useCase;

    @BeforeEach
    void setUp() {
        questionTopicRepository = mock(QuestionTopicRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateQuestionTopicUseCase(questionTopicRepository, questionBankRepository, userContextPort);
    }

    @Test
    void update_should_normalize_and_save_topic() {
        var userId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var topic = topic(topicId, bankId);

        when(questionBankRepository.existsById(bankId)).thenReturn(true);
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(questionTopicRepository.save(any(QuestionTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        var result = useCase.execute(new UpdateQuestionTopicCommand(topicId, bankId, "  Updated   topic  ", "  Updated   desc  "));

        assertThat(result.id()).isEqualTo(topicId);
        assertThat(topic.getName()).isEqualTo("Updated topic");
        assertThat(topic.getDescription()).isEqualTo("Updated desc");
        assertThat(topic.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void update_should_throw_when_bank_missing() {
        var bankId = UUID.randomUUID();
        when(questionBankRepository.existsById(bankId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> useCase.execute(new UpdateQuestionTopicCommand(UUID.randomUUID(), bankId, "Topic", null)));
    }

    private QuestionTopic topic(UUID id, UUID bankId) {
        return new QuestionTopic(id, bankId, "TOPIC", "Topic", null, QuestionTopicStatus.DRAFT,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
