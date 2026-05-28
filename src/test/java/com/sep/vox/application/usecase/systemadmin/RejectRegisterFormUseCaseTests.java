package com.sep.vox.application.usecase.systemadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.RegisterFormRejectedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.systemadmin.RejectRegisterFormUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;

public class RejectRegisterFormUseCaseTests {

    private RegisterFormRepository registerFormRepository;
    private UserContextPort userContextPort;
    private EventPublisherPort eventPublisherPort;
    private RejectRegisterFormUseCase rejectRegisterFormUseCase;

    @BeforeEach
    void setUp() {
        registerFormRepository = mock(RegisterFormRepository.class);
        userContextPort = mock(UserContextPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        rejectRegisterFormUseCase = new RejectRegisterFormUseCase(
            registerFormRepository,
            userContextPort,
            eventPublisherPort
        );
    }

    @Test
    void reject_register_form_should_update_form_and_publish_rejected_event() {
        var currentUserId = UUID.randomUUID();
        var registerFormId = UUID.randomUUID();
        var registerForm = registerForm(registerFormId);
        var command = new RejectRegisterFormCommand(registerFormId, "  Missing   documents  ");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(registerFormRepository.findById(registerFormId)).thenReturn(Optional.of(registerForm));
        when(registerFormRepository.updateRejectedRegisterForm(
            eq(registerFormId),
            eq(currentUserId),
            eq("Missing documents"),
            any(OffsetDateTime.class)
        )).thenReturn(1);

        var result = rejectRegisterFormUseCase.execute(command);

        assertThat(result).isNull();

        var updatedAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(registerFormRepository).updateRejectedRegisterForm(
            eq(registerFormId),
            eq(currentUserId),
            eq("Missing documents"),
            updatedAtCaptor.capture()
        );
        assertThat(updatedAtCaptor.getValue()).isNotNull();

        var eventCaptor = ArgumentCaptor.forClass(RegisterFormRejectedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new RegisterFormRejectedEvent(
            "admin@example.com",
            "Missing documents"
        ));
    }

    @Test
    void reject_register_form_should_throw_when_register_form_is_not_found() {
        var currentUserId = UUID.randomUUID();
        var registerFormId = UUID.randomUUID();
        var command = new RejectRegisterFormCommand(registerFormId, "Missing documents");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(registerFormRepository.findById(registerFormId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> rejectRegisterFormUseCase.execute(command));

        verify(registerFormRepository).findById(registerFormId);
        verify(registerFormRepository, never()).updateRejectedRegisterForm(
            any(UUID.class),
            any(UUID.class),
            any(String.class),
            any(OffsetDateTime.class)
        );
        verifyNoInteractions(eventPublisherPort);
    }

    @Test
    void reject_register_form_should_throw_when_register_form_is_not_pending_or_not_found_during_update() {
        var currentUserId = UUID.randomUUID();
        var registerFormId = UUID.randomUUID();
        var registerForm = registerForm(registerFormId);
        var command = new RejectRegisterFormCommand(registerFormId, "Missing documents");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(registerFormRepository.findById(registerFormId)).thenReturn(Optional.of(registerForm));
        when(registerFormRepository.updateRejectedRegisterForm(
            eq(registerFormId),
            eq(currentUserId),
            eq("Missing documents"),
            any(OffsetDateTime.class)
        )).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> rejectRegisterFormUseCase.execute(command));

        verify(registerFormRepository).updateRejectedRegisterForm(
            eq(registerFormId),
            eq(currentUserId),
            eq("Missing documents"),
            any(OffsetDateTime.class)
        );
        verifyNoInteractions(eventPublisherPort);
    }

    private RegisterForm registerForm(UUID id) {
        var now = OffsetDateTime.now();
        return new RegisterForm(
            id,
            new FullName("Nguyen Van A"),
            new IdentityNumber("123456789"),
            new Phone("0987654321"),
            new Email("admin@example.com"),
            new DateOfBirth(LocalDate.of(2000, 5, 24)),
            "123 Street",
            new SchoolDomain("school.edu.vn"),
            "School Name",
            "456 School Street",
            new PostalCode("700000"),
            "Principal",
            new StudentCount(500),
            null,
            RegisterFormStatus.PENDING,
            now,
            now,
            null
        );
    }
}
