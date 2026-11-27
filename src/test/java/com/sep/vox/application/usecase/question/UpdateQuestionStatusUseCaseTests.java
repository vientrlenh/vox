package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.UpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * The endpoint that updates the status of a single question.
 *
 * <p>The business rules moved to {@code QuestionStatusTransition}, shared with the bulk path;
 * this test class guards the translation of the rejection reason back into an exception, because that decides the
 * HTTP status the client receives (404 / 403 / 400) and must not change during refactors.
 */
class UpdateQuestionStatusUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionBankRepository questionBankRepository;
    private QuestionCollaboratorRepository questionCollaboratorRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private UpdateQuestionStatusUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID bankId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        questionCollaboratorRepository = mock(QuestionCollaboratorRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        UserRoleQueryRepository userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(any())).thenReturn(List.of());
        when(questionCollaboratorRepository.findByQuestionIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(questionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase = new UpdateQuestionStatusUseCase(
            questionRepository,
            questionBankRepository,
            questionCollaboratorRepository,
            new QuestionStatusActorResolver(userContextPort, schoolUserRepository, userRoleQueryRepository));
    }

    @Test
    void should_move_the_question_to_the_new_status() {
        Question question = question(QuestionStatus.DRAFT);
        mockQuestion(question);

        QuestionDto result = useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null));

        assertThat(result.status()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW.name());
        assertThat(result.updatedBy()).isEqualTo(currentUserId);
    }

    @Test
    void should_normalize_a_lowercase_action() {
        Question question = question(QuestionStatus.DRAFT);
        mockQuestion(question);

        QuestionDto result = useCase.execute(new UpdateQuestionStatusCommand(question.getId(), " submit ", null));

        assertThat(result.status()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW.name());
    }

    @Test
    void should_throw_not_found_when_the_question_does_not_exist() {
        when(questionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(UUID.randomUUID(), "SUBMIT", null)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Question not found");
    }

    @Test
    void should_throw_not_found_when_the_bank_does_not_exist() {
        Question question = question(QuestionStatus.DRAFT);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionBankRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Question bank not found");
    }

    @Test
    void should_throw_forbidden_when_the_actor_is_not_allowed() {
        Question question = question(QuestionStatus.DRAFT);
        question.setCreatedBy(UUID.randomUUID().toString());
        mockQuestion(question);

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Cannot submit for review: you are not the creator or a collaborator with edit rights on this question");
    }

    @Test
    void should_throw_invalid_state_when_the_status_does_not_allow_the_action() {
        Question question = question(QuestionStatus.SUBMITTED_FOR_REVIEW);
        mockQuestion(question);

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot submit for review: the question is in status \"Awaiting review\", "
                + "this action only applies to questions in status \"Draft\" or \"Revision requested\"");
    }

    @Test
    void should_throw_invalid_state_for_an_unknown_action() {
        Question question = question(QuestionStatus.DRAFT);
        mockQuestion(question);

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "DEMOLISH", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid action");
    }

    private void mockQuestion(Question question) {
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank()));
    }

    private Question question(QuestionStatus status) {
        return Question.builder()
                .id(UUID.randomUUID())
                .questionBankId(bankId)
                .questionTopicId(UUID.randomUUID())
                .code("Q-1")
                .instructionText(null)
                .questionText("Câu hỏi")
                .promptText(null)
                .preparationText(null)
                .type(QuestionType.SHORT_ANSWER)
                .preparationTimeSeconds(10)
                .minResponseSeconds(10)
                .maxResponseSeconds(60)
                .sharing(QuestionSharing.PRIVATE)
                .sourceQuestionId(null)
                .locked(false)
                .confidentiality(QuestionConfidentiality.OPEN)
                .securePoolId(null)
                .status(status)
                .createdBy(currentUserId.toString())
                .updatedBy(currentUserId.toString())
                .build();
    }

    private QuestionBank bank() {
        return QuestionBank.builder()
                .id(bankId)
                .languageId(UUID.randomUUID())
                .schoolId(null)
                .code("BANK-1")
                .name("Ngân hàng")
                .description(null)
                .ownerType(QuestionBankOwnerType.SYSTEM)
                .status(QuestionBankStatus.PUBLISHED)
                .createdBy(currentUserId.toString())
                .updatedBy(currentUserId.toString())
                .build();
    }
}
