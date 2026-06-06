package com.sep.vox.application.usecase.systemadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.usecase.registration.ViewRegisterFormDetailsUseCase;
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

public class ViewRegisterFormDetailsUseCaseTests {

    private RegisterFormRepository registerFormRepository;
    private ViewRegisterFormDetailsUseCase viewRegisterFormDetailsUseCase;

    @BeforeEach
    void setUp() {
        registerFormRepository = mock(RegisterFormRepository.class);
        viewRegisterFormDetailsUseCase = new ViewRegisterFormDetailsUseCase(registerFormRepository);
    }

    @Test
    void view_register_form_details_should_return_form_when_found() {
        var id = UUID.randomUUID();
        var registerForm = registerForm(id);
        when(registerFormRepository.findById(id)).thenReturn(Optional.of(registerForm));

        var result = viewRegisterFormDetailsUseCase.execute(new ViewRegisterFormDetailsQuery(id));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.contactFullName()).isEqualTo("Nguyen Van A");
        assertThat(result.identityNumber()).isEqualTo("123456789");
        assertThat(result.contactPhone()).isEqualTo("0987654321");
        assertThat(result.contactEmail()).isEqualTo("admin@example.com");
        assertThat(result.dateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 24));
        assertThat(result.contactAddress()).isEqualTo("123 Street");
        assertThat(result.schoolDomain()).isEqualTo("school.edu.vn");
        assertThat(result.schoolName()).isEqualTo("School Name");
        assertThat(result.schoolAddress()).isEqualTo("456 School Street");
        assertThat(result.postalCode()).isEqualTo("700000");
        assertThat(result.position()).isEqualTo("Principal");
        assertThat(result.studentCount()).isEqualTo(500);
        assertThat(result.reason()).isNull();
        assertThat(result.status()).isEqualTo("PENDING");
        verify(registerFormRepository).findById(id);
    }

    @Test
    void view_register_form_details_should_throw_when_form_is_not_found() {
        var id = UUID.randomUUID();
        when(registerFormRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> viewRegisterFormDetailsUseCase.execute(new ViewRegisterFormDetailsQuery(id))
        );

        verify(registerFormRepository).findById(id);
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
