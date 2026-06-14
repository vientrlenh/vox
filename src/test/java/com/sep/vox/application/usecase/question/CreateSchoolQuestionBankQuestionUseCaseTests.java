package com.sep.vox.application.usecase.question;

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

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.usecase.question.CreateSchoolQuestionBankQuestionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class CreateSchoolQuestionBankQuestionUseCaseTests {

    private UserRepository userRepository;
    private QuestionRepository questionRepository;
    private QuestionTopicRepository questionTopicRepository;
    private QuestionBankRepository questionBankRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private CreateSchoolQuestionBankQuestionUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionTopicRepository = mock(QuestionTopicRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new CreateSchoolQuestionBankQuestionUseCase(
            userRepository, questionRepository, questionTopicRepository, questionBankRepository, userContextPort, schoolUserRepository
        );
    }

    @Test
    void create_should_save_question_when_bank_and_topic_not_archived() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var questionId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(userId)));
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(new SchoolUser(schoolId, userId, OffsetDateTime.now(), null)));
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic(topicId, bankId, QuestionTopicStatus.PUBLISHED)));
        when(questionTopicRepository.isTopicBelongToSchool(topicId, schoolId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank(bankId, QuestionBankStatus.PUBLISHED)));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            var question = invocation.getArgument(0, Question.class);
            question.setId(questionId);
            return question;
        });

        var response = useCase.execute(new CreateSchoolQuestionBankQuestionCommand(
            topicId, "Q1", null, "Question", null, null, "SHORT_ANSWER", 10, 20, 30
        ));

        assertThat(response.questionId()).isEqualTo(questionId);
    }

    @Test
    void create_should_throw_when_bank_archived() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var bankId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(Optional.of(activeUser(userId)));
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(new SchoolUser(schoolId, userId, OffsetDateTime.now(), null)));
        when(questionTopicRepository.findById(topicId)).thenReturn(Optional.of(topic(topicId, bankId, QuestionTopicStatus.PUBLISHED)));
        when(questionTopicRepository.isTopicBelongToSchool(topicId, schoolId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank(bankId, QuestionBankStatus.ARCHIVED)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(new CreateSchoolQuestionBankQuestionCommand(
            topicId, "Q1", null, "Question", null, null, "SHORT_ANSWER", 10, 20, 30
        )));
    }

    private User activeUser(UUID id) {
        var user = new User();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private QuestionTopic topic(UUID topicId, UUID bankId, QuestionTopicStatus status) {
        return new QuestionTopic(topicId, bankId, "TOPIC", "Topic", null, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }

    private QuestionBank bank(UUID bankId, QuestionBankStatus status) {
        return new QuestionBank(bankId, UUID.randomUUID(), UUID.randomUUID(), "BANK", "Bank", null, QuestionBankOwnerType.SCHOOL, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
