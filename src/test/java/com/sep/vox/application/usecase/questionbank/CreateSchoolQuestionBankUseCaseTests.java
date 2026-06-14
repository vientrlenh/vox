package com.sep.vox.application.usecase.questionbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.model.school.SchoolUser;

class CreateSchoolQuestionBankUseCaseTests {

    private QuestionBankRepository questionBankRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private CreateSchoolQuestionBankUseCase useCase;

    @BeforeEach
    void setUp() {
        questionBankRepository = mock(QuestionBankRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSchoolQuestionBankUseCase(questionBankRepository, schoolUserRepository, userContextPort);
    }

    @Test
    void create_should_save_normalized_school_bank_for_current_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var bankId = UUID.randomUUID();
        var command = new CreateSchoolQuestionBankCommand(languageId, schoolId, "  sch-bank  ", "  School   Bank  ", "  Desc   value  ");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(new SchoolUser(schoolId, userId, OffsetDateTime.now(), null)));
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> {
            var bank = invocation.getArgument(0, QuestionBank.class);
            bank.setId(bankId);
            return bank;
        });

        var response = useCase.execute(command);

        assertThat(response.questionBankId()).isEqualTo(bankId);
        var captor = ArgumentCaptor.forClass(QuestionBank.class);
        verify(questionBankRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SCH-BANK");
        assertThat(captor.getValue().getName()).isEqualTo("School Bank");
        assertThat(captor.getValue().getDescription()).isEqualTo("Desc value");
        assertThat(captor.getValue().getOwnerType()).isEqualTo(QuestionBankOwnerType.SCHOOL);
        assertThat(captor.getValue().getSchoolId()).isEqualTo(schoolId);
    }

    @Test
    void create_should_throw_when_user_not_in_requested_school() {
        var userId = UUID.randomUUID();
        var command = new CreateSchoolQuestionBankCommand(UUID.randomUUID(), UUID.randomUUID(), "CODE", "Name", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(new SchoolUser(UUID.randomUUID(), userId, OffsetDateTime.now(), null)));

        assertThrows(ForbiddenException.class, () -> useCase.execute(command));
    }
}
