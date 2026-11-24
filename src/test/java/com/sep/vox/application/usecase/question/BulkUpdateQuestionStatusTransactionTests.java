package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.port.input.command.question.BulkUpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.usecase.question.BulkUpdateQuestionStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.support.IntegrationTest;

/**
 * Cập nhật trạng thái hàng loạt phải là "thành công một phần", không phải "được ăn cả ngã về không".
 *
 * <p>Vì sao cần transaction thật: bug gốc là {@code UnexpectedRollbackException}. Use case bulk mở
 * một transaction rồi gọi một use case {@code @Transactional} khác; use case bên trong tham gia
 * chính transaction đó, nên khi nó ném exception, Spring đánh dấu transaction là rollback-only.
 * Vòng lặp bulk bắt exception và trả về bình thường, nhưng lúc commit thì nổ 500 — và mọi câu hỏi
 * đã cập nhật thành công cũng bị cuốn theo. Unit test với mock KHÔNG bắt được: không có transaction
 * thật thì không có gì để đánh dấu rollback-only.
 *
 * <p>Lớp này KHÔNG {@code @Transactional}: transaction của use case phải là transaction ngoài cùng
 * thì mới tới lượt commit — chính là chỗ exception được ném ra.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
class BulkUpdateQuestionStatusTransactionTests {

    @Autowired
    private BulkUpdateQuestionStatusUseCase bulkUpdateQuestionStatusUseCase;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private UserContextPort userContextPort;

    private final UUID currentUserId = UUID.randomUUID();

    private UUID questionBankId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(currentUserId));
        when(userContextPort.isSystemAdmin()).thenReturn(false);

        questionBankId = transactionTemplate.execute(status -> questionBankRepository.save(QuestionBank.builder()
                .languageId(UUID.randomUUID())
                .schoolId(null)
                .code("BANK-" + UUID.randomUUID())
                .name("Ngân hàng kiểm thử")
                .description(null)
                .ownerType(QuestionBankOwnerType.SYSTEM)
                .status(QuestionBankStatus.PUBLISHED)
                .build()).getId());
        questionId = transactionTemplate.execute(status -> questionRepository.save(Question.builder()
                .questionBankId(questionBankId)
                .questionTopicId(UUID.randomUUID())
                .code("Q-" + UUID.randomUUID())
                .instructionText(null)
                .questionText("Câu hỏi kiểm thử")
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
                .status(QuestionStatus.DRAFT)
                .build()).getId());
    }

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            questionRepository.deleteById(questionId);
            questionBankRepository.deleteById(questionBankId);
        });
    }

    @Test
    void should_commit_the_valid_questions_when_part_of_the_batch_fails() {
        UUID missingQuestionId = UUID.randomUUID();
        BulkUpdateQuestionStatusCommand command = new BulkUpdateQuestionStatusCommand(
            List.of(questionId, missingQuestionId), "SUBMIT", null);

        assertThatCode(() -> {
            BulkUpdateQuestionStatusResponse response = bulkUpdateQuestionStatusUseCase.execute(command);

            assertThat(response.updated()).hasSize(1);
            assertThat(response.updated().getFirst().id()).isEqualTo(questionId);
            assertThat(response.failed()).hasSize(1);
            assertThat(response.failed().getFirst().questionId()).isEqualTo(missingQuestionId);
        }).doesNotThrowAnyException();

        // Pins the most important part: a valid question must remain in the DB after the commit.
        // Before the fix, the transaction was marked rollback-only, so this change disappeared.
        Question reloaded = transactionTemplate.execute(status -> questionRepository.findById(questionId).orElseThrow());
        assertThat(reloaded.getStatus()).isEqualTo(QuestionStatus.SUBMITTED_FOR_REVIEW);
    }
}
