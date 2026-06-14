package com.sep.vox.application.usecase.questionbank;

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
import com.sep.vox.application.port.input.command.UpdateQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionBankPermissionQuery;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;

class UpdateQuestionBankUseCaseTests {

    private QuestionBankRepository questionBankRepository;
    private QuestionBankPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private UpdateQuestionBankUseCase useCase;

    @BeforeEach
    void setUp() {
        questionBankRepository = mock(QuestionBankRepository.class);
        permissionQuery = mock(QuestionBankPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateQuestionBankUseCase(questionBankRepository, permissionQuery, userContextPort);
    }

    @Test
    void update_should_normalize_and_save_bank() {
        var userId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var bank = questionBank(bankId);
        var command = new UpdateQuestionBankCommand(bankId, "  Updated   Name  ", "  Updated   description  ", true);

        when(permissionQuery.canUpdateBank(bankId)).thenReturn(true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(command);

        assertThat(result.id()).isEqualTo(bankId);
        assertThat(bank.getName()).isEqualTo("Updated Name");
        assertThat(bank.getDescription()).isEqualTo("Updated description");
        assertThat(bank.getUpdatedBy()).isEqualTo(userId);
        assertThat(bank.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_should_throw_when_permission_denied() {
        var bankId = UUID.randomUUID();
        when(permissionQuery.canUpdateBank(bankId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new UpdateQuestionBankCommand(bankId, "Name", null, true)));
    }

    private QuestionBank questionBank(UUID id) {
        return new QuestionBank(
            id, UUID.randomUUID(), null, "BANK", "Bank", "Desc", QuestionBankOwnerType.SYSTEM, QuestionBankStatus.DRAFT,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
