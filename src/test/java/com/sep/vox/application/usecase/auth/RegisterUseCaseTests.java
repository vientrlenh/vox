package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.RegisterCommand;
import com.sep.vox.application.port.input.usecase.registration.RegisterUseCase;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;

public class RegisterUseCaseTests {

    private RegisterFormRepository registerFormRepository;
    private RegisterUseCase registerUseCase;

    @BeforeEach
    void setUp() {
        registerFormRepository = mock(RegisterFormRepository.class);
        registerUseCase = new RegisterUseCase(registerFormRepository);
    }

    @Test
    void register_should_save_pending_register_form_when_command_is_valid() {
        var command = validCommand();
        when(registerFormRepository.save(any(RegisterForm.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = registerUseCase.execute(command);

        var captor = ArgumentCaptor.forClass(RegisterForm.class);
        verify(registerFormRepository).save(captor.capture());

        var savedForm = captor.getValue();
        assertThat(response).isNull();
        assertThat(savedForm.getContactFullName().value()).isEqualTo("Nguyen Van A");
        assertThat(savedForm.getIdentityNumber().value()).isEqualTo("123456789");
        assertThat(savedForm.getContactPhone().value()).isEqualTo("0987654321");
        assertThat(savedForm.getContactEmail().value()).isEqualTo("admin@example.com");
        assertThat(savedForm.getDateOfBirth().value()).isEqualTo(command.dateOfBirth());
        assertThat(savedForm.getContactAddress()).isEqualTo("123 Street");
        assertThat(savedForm.getSchoolDomain().value()).isEqualTo("school.edu.vn");
        assertThat(savedForm.getSchoolName()).isEqualTo("School Name");
        assertThat(savedForm.getSchoolAddress()).isEqualTo("456 School Street");
        assertThat(savedForm.getPostalCode().value()).isEqualTo("700000");
        assertThat(savedForm.getPosition()).isEqualTo("Principal");
        assertThat(savedForm.getStudentCount().value()).isEqualTo(500);
        assertThat(savedForm.getReason()).isNull();
        assertThat(savedForm.getStatus()).isEqualTo(RegisterFormStatus.PENDING);
        assertThat(savedForm.getCreatedAt()).isNotNull();
        assertThat(savedForm.getUpdatedAt()).isNotNull();
        assertThat(savedForm.getUpdatedBy()).isNull();
    }

    private RegisterCommand validCommand() {
        return new RegisterCommand(
            "  Nguyen   Van   A  ",
            " 123 456 789 ",
            " 098-765.43 21 ",
            " Admin@Example.COM ",
            LocalDate.now().minusYears(20),
            "  123   Street  ",
            " SCHOOL.EDU.VN ",
            "  School   Name  ",
            "  456   School   Street  ",
            " 700 000 ",
            "  Principal  ",
            500
        );
    }
}
