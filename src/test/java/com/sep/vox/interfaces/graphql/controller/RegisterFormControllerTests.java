package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.query.ViewRegisterFormsQuery;
import com.sep.vox.application.port.input.usecase.systemadmin.ViewRegisterFormDetailsUseCase;
import com.sep.vox.application.port.input.usecase.systemadmin.ViewRegisterFormsUseCase;
import com.sep.vox.domain.dto.RegisterFormDto;
import com.sep.vox.domain.util.PageResult;

public class RegisterFormControllerTests {

    @Test
    void register_forms_should_return_page_from_use_case() {
        var registerFormsUseCase = mock(ViewRegisterFormsUseCase.class);
        var registerFormDetailsUseCase = mock(ViewRegisterFormDetailsUseCase.class);
        var controller = new RegisterFormController(registerFormsUseCase, registerFormDetailsUseCase);
        var form = new RegisterFormDto(
            UUID.randomUUID(),
            "Nguyen Van A",
            "123456789",
            "0987654321",
            "admin@example.com",
            LocalDate.of(2000, 5, 24),
            "123 Street",
            "school.edu.vn",
            "School Name",
            "456 School Street",
            "700000",
            "Principal",
            500,
            null,
            "PENDING"
        );
        var page = new PageResult<>(List.of(form), 0, 20, 1, 1);

        when(registerFormsUseCase.execute(new ViewRegisterFormsQuery(0, 20))).thenReturn(page);

        var result = controller.registerForms(0, 20);

        assertThat(result).isEqualTo(page);
        assertThat(result.content()).containsExactly(form);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(registerFormsUseCase).execute(new ViewRegisterFormsQuery(0, 20));
    }

    @Test
    void register_form_should_return_details_from_use_case() {
        var registerFormsUseCase = mock(ViewRegisterFormsUseCase.class);
        var registerFormDetailsUseCase = mock(ViewRegisterFormDetailsUseCase.class);
        var controller = new RegisterFormController(registerFormsUseCase, registerFormDetailsUseCase);
        var id = UUID.randomUUID();
        var form = new RegisterFormDto(
            id,
            "Nguyen Van A",
            "123456789",
            "0987654321",
            "admin@example.com",
            LocalDate.of(2000, 5, 24),
            "123 Street",
            "school.edu.vn",
            "School Name",
            "456 School Street",
            "700000",
            "Principal",
            500,
            null,
            "PENDING"
        );

        when(registerFormDetailsUseCase.execute(new ViewRegisterFormDetailsQuery(id))).thenReturn(form);

        var result = controller.registerForm(id);

        assertThat(result).isEqualTo(form);
        assertThat(result.id()).isEqualTo(id);
        verify(registerFormDetailsUseCase).execute(new ViewRegisterFormDetailsQuery(id));
    }
}
