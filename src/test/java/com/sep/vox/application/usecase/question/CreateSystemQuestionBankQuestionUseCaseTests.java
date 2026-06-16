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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.UserRepository;

class CreateSystemQuestionBankQuestionUseCaseTests {

    private UserRepository userRepository;
    private QuestionRepository questionRepository;
    private QuestionTopicRepository questionTopicRepository;
    private QuestionBankRepository questionBankRepository;
    private UserContextPort userContextPort;
    private CreateSystemQuestionBankQuestionUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionTopicRepository = mock(QuestionTopicRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSystemQuestionBankQuestionUseCase(
            userRepository, questionBankRepository, questionRepository, questionTopicRepository, userContextPort
        );
    }

    @Test
    void create_should_save_normalized_question_for_accessible_topic() {
        var userId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var questionId = UUID.randomUUID();
        var command = new CreateSystemQuestionBankQuestionCommand(
            topicId, "  q-01  ", "  Instruction  ", "  Question   text  ", "  Prompt  ", "  Preparation  ",
            "  SHORT_ANSWER  ", "  CENTRAL_EXAM_DRAFT  ", "  REVIEWER_ONLY  ", 15, 30, 60
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(userId)));
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(activeTopic(topicId, bankId)));
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(systemBank(bankId)));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            var question = invocation.getArgument(0, Question.class);
            question.setId(questionId);
            return question;
        });

        var response = useCase.execute(command);

        assertThat(response.questionId()).isEqualTo(questionId);
        var captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("Q-01");
        assertThat(captor.getValue().getQuestionText()).isEqualTo("Question text");
        assertThat(captor.getValue().getType()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(captor.getValue().getScope()).isEqualTo(QuestionScope.CENTRAL_EXAM_DRAFT);
        assertThat(captor.getValue().getVisibility()).isEqualTo(QuestionVisibility.REVIEWER_ONLY);
        assertThat(captor.getValue().getStatus()).isEqualTo(QuestionStatus.DRAFT);
    }

    @Test
    void create_should_throw_when_topic_is_not_in_system_bank() {
        var userId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var bankId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(userId)));
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(activeTopic(topicId, bankId)));
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(schoolBank(bankId)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(new CreateSystemQuestionBankQuestionCommand(
            topicId, "Q1", null, "Text", null, null, "SHORT_ANSWER", "QUESTION_BANK", "BANK_VISIBLE", 10, 20, 30
        )));
    }

    private User activeUser(UUID id) {
        var user = new User();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private QuestionTopic activeTopic(UUID id, UUID bankId) {
        return new QuestionTopic(id, bankId, "TOPIC", "Topic", null, QuestionTopicStatus.PUBLISHED,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }

    private QuestionBank systemBank(UUID id) {
        return new QuestionBank(
            id,
            UUID.randomUUID(),
            null,
            "SYS_BANK",
            "System Bank",
            null,
            QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.PUBLISHED,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private QuestionBank schoolBank(UUID id) {
        return new QuestionBank(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SCHOOL_BANK",
            "School Bank",
            null,
            QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
