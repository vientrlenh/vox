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

import com.sep.vox.application.port.input.command.BulkUpdateQuestionStatusCommand;
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
 * Cập nhật trạng thái hàng loạt.
 *
 * <p>Bất biến: một câu hỏi bị từ chối KHÔNG được làm hỏng cả request. Trước đây use case này gọi
 * {@code UpdateQuestionStatusUseCase} — một bean {@code @Transactional} khác — nên exception của
 * câu lỗi đánh dấu transaction dùng chung là rollback-only và request kết thúc bằng HTTP 500.
 * Xem thêm {@code BulkUpdateQuestionStatusTransactionTests} cho phần transaction thật.
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
            "Không thể gửi duyệt: câu hỏi đang ở trạng thái \"Chờ duyệt\", "
                + "thao tác này chỉ áp dụng cho câu hỏi ở trạng thái \"Bản nháp\" hoặc \"Yêu cầu sửa\"");
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
        assertThat(response.failed().getFirst().reason()).isEqualTo("Không tìm thấy câu hỏi");
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
        assertThat(response.failed().getFirst().reason()).isEqualTo("Không tìm thấy ngân hàng câu hỏi");
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
        otherAuthorQuestion.setCreatedBy(UUID.randomUUID());
        mockQuestions(otherAuthorQuestion);

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(otherAuthorQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).isEmpty();
        assertThat(response.failed().getFirst().reason()).isEqualTo(
            "Không thể gửi duyệt: bạn không phải người tạo hoặc người cộng tác có quyền sửa câu hỏi này");
    }

    @Test
    void should_treat_an_editor_collaborator_as_allowed_to_submit() {
        Question otherAuthorQuestion = question(QuestionStatus.DRAFT);
        otherAuthorQuestion.setCreatedBy(UUID.randomUUID());
        mockQuestions(otherAuthorQuestion);
        when(questionCollaboratorRepository.findByQuestionIdIn(anyCollection())).thenReturn(List.of(
            new QuestionCollaborator(UUID.randomUUID(), currentUserId, otherAuthorQuestion.getId(),
                QuestionCollaboratorPermission.CAN_EDIT, Instant.now())));

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
        otherAuthorQuestion.setCreatedBy(UUID.randomUUID());
        mockQuestions(otherAuthorQuestion);
        when(questionCollaboratorRepository.findByQuestionIdIn(anyCollection())).thenReturn(List.of(
            new QuestionCollaborator(UUID.randomUUID(), UUID.randomUUID(), otherAuthorQuestion.getId(),
                QuestionCollaboratorPermission.CAN_EDIT, Instant.now())));

        BulkUpdateQuestionStatusResponse response = useCase.execute(new BulkUpdateQuestionStatusCommand(
            List.of(otherAuthorQuestion.getId()), "SUBMIT", null));

        assertThat(response.updated()).isEmpty();
        assertThat(response.failed().getFirst().reason()).isEqualTo(
            "Không thể gửi duyệt: bạn không phải người tạo hoặc người cộng tác có quyền sửa câu hỏi này");
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
            .hasMessage("Danh sách câu hỏi không được để trống");
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
        Instant now = Instant.now();
        return new Question(
            UUID.randomUUID(), bankId, UUID.randomUUID(), "Q-1", null, "Câu hỏi", null, null,
            QuestionType.SHORT_ANSWER, 10, 10, 60, QuestionSharing.PRIVATE, null, false,
            QuestionConfidentiality.OPEN, null, status, now, now, currentUserId, currentUserId);
    }

    private QuestionBank bank() {
        Instant now = Instant.now();
        return new QuestionBank(
            bankId, UUID.randomUUID(), null, "BANK-1", "Ngân hàng", null,
            QuestionBankOwnerType.SYSTEM, QuestionBankStatus.PUBLISHED, now, now, currentUserId, currentUserId);
    }
}
