package com.sep.vox.application.usecase.questiontopic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

class CreateQuestionTopicUseCaseTests {

    private QuestionTopicRepository questionTopicRepository;
    private QuestionBankRepository questionBankRepository;
    private UserContextPort userContextPort;
    private CreateQuestionTopicUseCase useCase;

    @BeforeEach
    void setUp() {
        questionTopicRepository = mock(QuestionTopicRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateQuestionTopicUseCase(questionTopicRepository, questionBankRepository, userContextPort);
    }

    @Test
    void create_should_generate_normalized_code_and_save_draft_topic() {
        var bankId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var command = new CreateQuestionTopicCommand(bankId, "  Speaking   topic  ", "  Topic   description  ");

        when(questionBankRepository.existsById(bankId)).thenReturn(true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(questionTopicRepository.save(any(QuestionTopic.class))).thenAnswer(invocation -> {
            var topic = invocation.getArgument(0, QuestionTopic.class);
            topic.setId(topicId);
            return topic;
        });

        var response = useCase.execute(command);

        assertThat(response.questionTopicId()).isEqualTo(topicId);
        var captor = ArgumentCaptor.forClass(QuestionTopic.class);
        verify(questionTopicRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SPEAKING TOPIC");
        assertThat(captor.getValue().getName()).isEqualTo("Speaking topic");
        assertThat(captor.getValue().getDescription()).isEqualTo("Topic description");
        assertThat(captor.getValue().getStatus()).isEqualTo(QuestionTopicStatus.DRAFT);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void create_should_throw_when_bank_not_found() {
        var bankId = UUID.randomUUID();
        when(questionBankRepository.existsById(bankId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> useCase.execute(new CreateQuestionTopicCommand(bankId, "Topic", null)));
    }
}
