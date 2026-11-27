package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.question.BulkUpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.question.BulkUpdateQuestionStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusFailure;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.question.QuestionStatusTransition.RejectionCode;

/**
 * Bulk status update.
 *
 * <p>Invariant: one rejected question must NOT break the whole request. This use case used to call
 * {@code UpdateQuestionStatusUseCase} -- another {@code @Transactional} bean -- so the exception of the
 * failing question marked the shared transaction rollback-only and the request ended with HTTP 500.
 * See also {@code BulkUpdateQuestionStatusTransactionTests} for the real-transaction part.
 */
class BulkUpdateQuestionStatusUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionBankRepository questionBankRepository;
    private QuestionCollaboratorRepository questionCollaboratorRepository;
    private UserContextPort userContextPort;
    private UserRoleQueryRepository userRoleQueryRepository;
    private SchoolUserRepository schoolUserRepository;
    private BulkUpdateQuestionStatusUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID bankId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        questionCollaboratorRepository = mock(QuestionCollaboratorRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(any())).thenReturn(List.of());
        when(questionCollaboratorRepository.findByQuestionIdIn(anyCollection())).thenReturn(List.of());
        mockSaveAllEchoesInput();

        useCase = new BulkUpdateQuestionStatusUseCase(
            questionRepository,
            questionBankRepository,
            questionCollaboratorRepository,
            new QuestionStatusActorResolver(userContextPort, schoolUserRepository, userRoleQueryRepository));
    }

    @Test
    void should_return_partial_success_when_some_questions_fail() {
        Question draftQuestion = question(QuestionStatus.DRAFT);
        Question alreadySubmitted = question(QuestionStatus.SUBMITTED_FOR_REVIEW);
        mockQuestions(draftQuestion, alreadySubmitted);

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(draftQuestion.getId(), alreadySubmitted.getId()), "SUBMIT", null));

        assertThat(response.updated()).hasSize(1);
        assertThat(response.updated().getFirst().id()).isEqualTo(draftQuestion.getId());
        assertThat(response.updated().getFirst().status()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW.name());
        assertThat(response.failed()).hasSize(1);
        assertThat(response.failed().getFirst().questionId()).isEqualTo(alreadySubmitted.getId());
        assertThat(response.failed().getFirst().reason()).isEqualTo(
            "Cannot submit for review: the question is in status \"Awaiting review\", "
                + "this action only applies to questions in status \"Draft\" or \"Revision requested\"");
    }

    /**
     * The bulk review screen must be able to list "which question, where it is, and why" straight
     * from the response. Previously there was only {@code questionId}, so the client had to look it
     * up in the list being viewed -- a question not on the current page vanished from the message.
     */
    @Test
    void should_describe_each_skipped_question_with_its_code_status_and_reason_code() {
        Question alreadySubmitted = question(QuestionStatus.SUBMITTED_FOR_REVIEW);
        mockQuestions(alreadySubmitted);

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(alreadySubmitted.getId()), "SUBMIT", null));

        BulkUpdateQuestionStatusFailure failure = response.failed().getFirst();
        assertThat(failure.questionCode()).isEqualTo(alreadySubmitted.getCode());
        assertThat(failure.currentStatus()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW.name());
        assertThat(failure.reasonCode()).isEqualTo(RejectionCode.INVALID_STATUS.name());
    }

    @Test
    void should_report_missing_question_without_aborting_the_batch() {
        Question draftQuestion = question(QuestionStatus.DRAFT);
        UUID missingQuestionId = UUID.randomUUID();
        mockQuestions(draftQuestion);

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(draftQuestion.getId(), missingQuestionId), "SUBMIT", null));

        assertThat(response.updated()).hasSize(1);
        assertThat(response.failed()).hasSize(1);
        assertThat(response.failed().getFirst().questionId()).isEqualTo(missingQuestionId);
        assertThat(response.failed().getFirst().reason()).isEqualTo("Question not found");
        assertThat(response.failed().getFirst().reasonCode())
            .isEqualTo(RejectionCode.QUESTION_NOT_FOUND.name());
    }

    @Test
    void should_report_a_missing_bank_without_aborting_the_batch() {
        Question orphanQuestion = question(QuestionStatus.DRAFT);
        when(questionRepository.findByIdIn(anyCollection())).thenReturn(List.of(orphanQuestion));
        when(questionBankRepository.findByIdIn(anyCollection())).thenReturn(List.of());

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(orphanQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).isEmpty();
        assertThat(response.failed()).hasSize(1);
        assertThat(response.failed().getFirst().reason()).isEqualTo("Question bank not found");
    }

    /**
     * The most common user scenario: select the whole page and click approve, and most of them are
     * in the wrong status.
     */
    @Test
    void should_not_throw_when_every_question_in_the_batch_fails() {
        Question first = question(QuestionStatus.DRAFT);
        Question second = question(QuestionStatus.DRAFT);
        mockQuestions(first, second);

        assertThatCode(() -> {
            BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
                List.of(first.getId(), second.getId()), "APPROVE", null));

            assertThat(response.updated()).isEmpty();
            assertThat(response.failed()).hasSize(2);
        }).doesNotThrowAnyException();
    }

    @Test
    void should_reject_questions_the_actor_may_not_touch() {
        Question otherAuthorQuestion = question(QuestionStatus.DRAFT);
        otherAuthorQuestion.setCreatedBy(UUID.randomUUID().toString());
        mockQuestions(otherAuthorQuestion);

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(otherAuthorQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).isEmpty();
        assertThat(response.failed().getFirst().reason()).isEqualTo(
            "Cannot submit for review: you are not the creator or a collaborator with edit rights on this question");
    }

    @Test
    void should_treat_an_editor_collaborator_as_allowed_to_submit() {
        Question otherAuthorQuestion = question(QuestionStatus.DRAFT);
        otherAuthorQuestion.setCreatedBy(UUID.randomUUID().toString());
        mockQuestions(otherAuthorQuestion);
        when(questionCollaboratorRepository.findByQuestionIdIn(anyCollection())).thenReturn(List.of(
            QuestionCollaborator.builder()
                    .id(UUID.randomUUID())
                    .userId(currentUserId)
                    .questionId(otherAuthorQuestion.getId())
                    .permission(QuestionCollaboratorPermission.CAN_EDIT)
                    .assignedAt(Instant.now())
                    .build()));

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(otherAuthorQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).hasSize(1);
        assertThat(response.failed()).isEmpty();
    }

    /**
     * The permissions of another collaborator must not leak onto the person performing the
     * operation.
     */
    @Test
    void should_ignore_edit_permission_granted_to_another_user() {
        Question otherAuthorQuestion = question(QuestionStatus.DRAFT);
        otherAuthorQuestion.setCreatedBy(UUID.randomUUID().toString());
        mockQuestions(otherAuthorQuestion);
        when(questionCollaboratorRepository.findByQuestionIdIn(anyCollection())).thenReturn(List.of(
            QuestionCollaborator.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .questionId(otherAuthorQuestion.getId())
                    .permission(QuestionCollaboratorPermission.CAN_EDIT)
                    .assignedAt(Instant.now())
                    .build()));

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(otherAuthorQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).isEmpty();
        assertThat(response.failed().getFirst().reason()).isEqualTo(
            "Cannot submit for review: you are not the creator or a collaborator with edit rights on this question");
    }

    @Test
    void should_skip_duplicate_ids() {
        Question draftQuestion = question(QuestionStatus.DRAFT);
        mockQuestions(draftQuestion);

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(draftQuestion.getId(), draftQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).hasSize(1);
        assertThat(response.failed()).isEmpty();
    }

    /**
     * The user context and the question data must be loaded in batches, not one query round per
     * question -- this is the N+1 part that came along with the rollback bug.
     */
    @Test
    void should_load_context_once_and_save_in_one_batch() {
        Question first = question(QuestionStatus.DRAFT);
        Question second = question(QuestionStatus.DRAFT);
        mockQuestions(first, second);

        useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(first.getId(), second.getId()), "SUBMIT", null));

        verify(userRoleQueryRepository, times(1)).findByUserIdWithRoleInfo(currentUserId);
        verify(questionRepository, times(1)).findByIdIn(anyCollection());
        verify(questionRepository, times(1)).saveAll(anyCollection());
        verify(questionRepository, never()).findById(any());
        verify(questionRepository, never()).save(any());
        verify(questionCollaboratorRepository, never()).findByQuestionIdAndUserId(any(), any());
    }

    @Test
    void should_refuse_an_empty_batch() {
        assertThatThrownBy(() -> useCase.execute(new BulkUpdateQuestionStatusCommand(List.of(), "SUBMIT", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The question list must not be empty");
    }

    private void mockQuestions(Question... questions) {
        when(questionRepository.findByIdIn(anyCollection())).thenReturn(List.of(questions));
        when(questionBankRepository.findByIdIn(anyCollection())).thenReturn(List.of(bank()));
    }

    private void mockSaveAllEchoesInput() {
        when(questionRepository.saveAll(anyCollection()))
            .thenAnswer(invocation -> List.copyOf(invocation.getArgument(0, Collection.class)));
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
