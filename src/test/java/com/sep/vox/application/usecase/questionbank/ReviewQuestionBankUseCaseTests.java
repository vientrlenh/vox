package com.sep.vox.application.usecase.questionbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.ReviewQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.ReviewQuestionBankUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.QuestionBankPermissionQuery;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;

class ReviewQuestionBankUseCaseTests {

    private QuestionBankRepository questionBankRepository;
    private QuestionBankPermissionQuery permissionQuery;
    private UserContextPort userContextPort;
    private ReviewQuestionBankUseCase useCase;

    @BeforeEach
    void setUp() {
        questionBankRepository = mock(QuestionBankRepository.class);
        permissionQuery = mock(QuestionBankPermissionQuery.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ReviewQuestionBankUseCase(questionBankRepository, permissionQuery, userContextPort);
    }

    @Test
    void review_should_publish_bank_when_permitted() {
        var bankId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var bank = bank(bankId, QuestionBankStatus.DRAFT);

        when(permissionQuery.canPublishBank(bankId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        var response = useCase.execute(new ReviewQuestionBankCommand(bankId, QuestionBankStatus.PUBLISHED));

        assertThat(response.questionBankId()).isEqualTo(bankId);
        assertThat(bank.getStatus()).isEqualTo(QuestionBankStatus.PUBLISHED);
        assertThat(bank.getUpdatedBy()).isEqualTo(userId);
        verify(permissionQuery).canPublishBank(bankId);
        verify(permissionQuery, never()).canArchiveBank(bankId);
        verify(permissionQuery, never()).canRestoreBank(bankId);
    }

    @Test
    void review_should_archive_bank_when_permitted() {
        var bankId = UUID.randomUUID();
        var bank = bank(bankId, QuestionBankStatus.PUBLISHED);

        when(permissionQuery.canArchiveBank(bankId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        var response = useCase.execute(new ReviewQuestionBankCommand(bankId, QuestionBankStatus.ARCHIVED));

        assertThat(response.questionBankId()).isEqualTo(bankId);
        assertThat(bank.getStatus()).isEqualTo(QuestionBankStatus.ARCHIVED);
        verify(permissionQuery).canArchiveBank(bankId);
        verify(permissionQuery, never()).canPublishBank(bankId);
        verify(permissionQuery, never()).canRestoreBank(bankId);
    }

    @Test
    void review_should_restore_bank_when_permitted() {
        var bankId = UUID.randomUUID();
        var bank = bank(bankId, QuestionBankStatus.ARCHIVED);

        when(permissionQuery.canRestoreBank(bankId)).thenReturn(true);
        when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());

        var response = useCase.execute(new ReviewQuestionBankCommand(bankId, QuestionBankStatus.DRAFT));

        assertThat(response.questionBankId()).isEqualTo(bankId);
        assertThat(bank.getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
        verify(permissionQuery).canRestoreBank(bankId);
        verify(permissionQuery, never()).canPublishBank(bankId);
        verify(permissionQuery, never()).canArchiveBank(bankId);
    }

    @Test
    void review_should_throw_when_not_permitted() {
        var bankId = UUID.randomUUID();
        when(permissionQuery.canArchiveBank(bankId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ReviewQuestionBankCommand(bankId, QuestionBankStatus.ARCHIVED)));
        verify(permissionQuery).canArchiveBank(bankId);
        verifyNoInteractions(questionBankRepository, userContextPort);
    }

    private QuestionBank bank(UUID id, QuestionBankStatus status) {
        return new QuestionBank(id, UUID.randomUUID(), null, "BANK", "Bank", null, QuestionBankOwnerType.SYSTEM, status,
            OffsetDateTime.now(), OffsetDateTime.now(), UUID.randomUUID(), UUID.randomUUID());
    }
}
