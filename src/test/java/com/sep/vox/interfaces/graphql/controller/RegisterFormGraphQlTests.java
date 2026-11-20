package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.registration.ViewRegisterFormDetailsUseCase;
import com.sep.vox.application.port.input.usecase.registration.ViewRegisterFormsUseCase;
import com.sep.vox.domain.dto.RegisterFormDto;
import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;
import com.sep.vox.interfaces.graphql.config.RegisterFormGraphQlDataLoaderConfig;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends a register form query through the real schema and asserts the typed values: the {@code Date} scalar for the
 * date of birth, the {@code RegisterFormStatus} enum and the {@code DateTime} scalar of the documents loaded in batch.
 */
@GraphQlControllerTest(RegisterFormController.class)
@Import(RegisterFormGraphQlDataLoaderConfig.class)
class RegisterFormGraphQlTests {

    private static final UUID FORM_ID = UUID.randomUUID();

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewRegisterFormsUseCase viewRegisterFormsUseCase;
    @MockitoBean
    private ViewRegisterFormDetailsUseCase viewRegisterFormDetailsUseCase;
    @MockitoBean
    private RegisterFormDocumentRepository registerFormDocumentRepository;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void register_form_should_return_date_status_enum_and_document_date_time() {
        when(viewRegisterFormDetailsUseCase.execute(any())).thenReturn(registerForm());
        when(registerFormDocumentRepository.findByRegisterFormIdIn(any())).thenReturn(List.of(
            new RegisterFormDocument(UUID.randomUUID(), FORM_ID, "https://files.example/doc.pdf",
                Instant.parse("2026-07-28T02:30:00Z"))));

        graphQlTester.document("{ registerForm(id: \"%s\") { dateOfBirth status documents { createdAt } } }"
                .formatted(FORM_ID))
            .execute()
            .errors().verify()
            .path("registerForm.dateOfBirth").entity(String.class).isEqualTo("1980-05-17")
            .path("registerForm.status").entity(String.class).isEqualTo("AUTO_APPROVED")
            .path("registerForm.documents[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void register_form_should_return_null_for_an_absent_date_of_birth() {
        when(viewRegisterFormDetailsUseCase.execute(any())).thenReturn(registerFormWithoutDateOfBirth());

        graphQlTester.document("{ registerForm(id: \"%s\") { dateOfBirth status } }".formatted(FORM_ID))
            .execute()
            .errors().verify()
            .path("registerForm.dateOfBirth").valueIsNull()
            .path("registerForm.status").entity(String.class).isEqualTo("PENDING");
    }

    private static RegisterFormDto registerForm() {
        return new RegisterFormDto(FORM_ID, null, "School", "school.example", null, null, null, "Full Name", null,
            null, null, LocalDate.of(1980, 5, 17), null, null, null, 10, null, null,
            RegisterFormStatus.AUTO_APPROVED);
    }

    private static RegisterFormDto registerFormWithoutDateOfBirth() {
        return new RegisterFormDto(FORM_ID, null, "School", "school.example", null, null, null, "Full Name", null,
            null, null, null, null, null, null, 10, null, null, RegisterFormStatus.PENDING);
    }
}
