package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.usecase.TestSchoolUserRepository;
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

/**
 * Endpoint cập nhật trạng thái một câu hỏi.
 *
 * <p>Quy tắc nghiệp vụ đã chuyển sang {@code QuestionStatusTransition} dùng chung với đường đi
 * hàng loạt; lớp test này canh phần dịch ngược lý do từ chối thành exception, vì đó là thứ quyết
 * định HTTP status client nhận được (404 / 403 / 400) và không được đổi khi refactor.
 */
class UpdateQuestionStatusUseCaseTests {

    private QuestionRepository questionRepository;
    private QuestionBankRepository questionBankRepository;
    private QuestionCollaboratorRepository questionCollaboratorRepository;
    private UserContextPort userContextPort;
    private UpdateQuestionStatusUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID bankId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        questionBankRepository = mock(QuestionBankRepository.class);
        questionCollaboratorRepository = mock(QuestionCollaboratorRepository.class);
        userContextPort = mock(UserContextPort.class);
        var userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        var schoolUserRepository = TestSchoolUserRepository.create();
        TestSchoolUserRepository.remember(currentUserId, schoolId);
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
        var question = question(QuestionStatus.DRAFT);
        mockQuestion(question);

        var result = useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null));

        assertThat(result.status()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW.name());
        assertThat(result.updatedBy()).isEqualTo(currentUserId);
    }

    @Test
    void should_normalize_a_lowercase_action() {
        var question = question(QuestionStatus.DRAFT);
        mockQuestion(question);

        var result = useCase.execute(new UpdateQuestionStatusCommand(question.getId(), " submit ", null));

        assertThat(result.status()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW.name());
    }

    @Test
    void should_throw_not_found_when_the_question_does_not_exist() {
        when(questionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(UUID.randomUUID(), "SUBMIT", null)))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Không tìm thấy câu hỏi");
    }

    @Test
    void should_throw_not_found_when_the_bank_does_not_exist() {
        var question = question(QuestionStatus.DRAFT);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionBankRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null)))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Không tìm thấy ngân hàng câu hỏi");
    }

    @Test
    void should_throw_forbidden_when_the_actor_is_not_allowed() {
        var question = question(QuestionStatus.DRAFT);
        question.setCreatedBy(UUID.randomUUID());
        mockQuestion(question);

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("Không thể gửi duyệt: bạn không phải người tạo hoặc người cộng tác có quyền sửa câu hỏi này");
    }

    @Test
    void should_throw_invalid_state_when_the_status_does_not_allow_the_action() {
        var question = question(QuestionStatus.SUBMITTED_FOR_REVIEW);
        mockQuestion(question);

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "SUBMIT", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Không thể gửi duyệt: câu hỏi đang ở trạng thái \"Chờ duyệt\", "
                + "thao tác này chỉ áp dụng cho câu hỏi ở trạng thái \"Bản nháp\" hoặc \"Yêu cầu sửa\"");
    }

    @Test
    void should_throw_invalid_state_for_an_unknown_action() {
        var question = question(QuestionStatus.DRAFT);
        mockQuestion(question);

        assertThatThrownBy(() -> useCase.execute(new UpdateQuestionStatusCommand(question.getId(), "DEMOLISH", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Action không hợp lệ");
    }

    private void mockQuestion(Question question) {
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank()));
    }

    private Question question(QuestionStatus status) {
        var now = Instant.now();
        return new Question(
            UUID.randomUUID(), bankId, UUID.randomUUID(), "Q-1", null, "Câu hỏi", null, null,
            QuestionType.SHORT_ANSWER, 10, 10, 60, QuestionSharing.PRIVATE, null, false,
            QuestionConfidentiality.OPEN, null, status, now, now, currentUserId, currentUserId);
    }

    private QuestionBank bank() {
        var now = Instant.now();
        return new QuestionBank(
            bankId, UUID.randomUUID(), null, "BANK-1", "Ngân hàng", null,
            QuestionBankOwnerType.SYSTEM, QuestionBankStatus.PUBLISHED, now, now, currentUserId, currentUserId);
    }
}
