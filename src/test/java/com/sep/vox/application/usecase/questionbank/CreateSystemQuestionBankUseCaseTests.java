package com.sep.vox.application.usecase.questionbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.CreateSystemQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

class CreateSystemQuestionBankUseCaseTests {

    private QuestionBankRepository questionBankRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private UserContextPort userContextPort;
    private CreateSystemQuestionBankUseCase useCase;

    @BeforeEach
    void setUp() {
        questionBankRepository = mock(QuestionBankRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSystemQuestionBankUseCase(questionBankRepository, supportedLanguageRepository, userContextPort);
    }

    @Test
    void create_should_save_normalized_system_bank() {
        var userId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var command = new CreateSystemQuestionBankCommand(languageId, "  sys-bank  ", "  System   Bank  ", "  Main   description  ");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.existsByIdAndIsActive(languageId, true)).thenReturn(true);
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> {
            var bank = invocation.getArgument(0, QuestionBank.class);
            bank.setId(bankId);
            return bank;
        });

        var response = useCase.execute(command);

        assertThat(response.questionBankId()).isEqualTo(bankId);
        var captor = ArgumentCaptor.forClass(QuestionBank.class);
        verify(questionBankRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SYS-BANK");
        assertThat(captor.getValue().getName()).isEqualTo("System Bank");
        assertThat(captor.getValue().getDescription()).isEqualTo("Main description");
        assertThat(captor.getValue().getOwnerType()).isEqualTo(QuestionBankOwnerType.SYSTEM);
        assertThat(captor.getValue().getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void create_should_throw_when_language_is_inactive() {
        var command = new CreateSystemQuestionBankCommand(UUID.randomUUID(), "SYS", "Bank", null);
        when(supportedLanguageRepository.existsByIdAndIsActive(command.languageId(), true)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }
}
