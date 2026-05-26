package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.command.RegisterCommand;
import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.RegisterUseCase;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;

public class AuthControllerTests {

    @Test
    void login_should_return_ok_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase);
        var request = new LoginRequest("admin@example.com", "password");
        var loginResponse = new LoginResponse("access-token", "refresh-token");

        when(loginUseCase.execute(new LoginCommand(request.login(), request.password())))
            .thenReturn(loginResponse);

        var response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(loginResponse);
        verify(loginUseCase).execute(new LoginCommand(request.login(), request.password()));
    }

    @Test
    void register_should_return_created_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase);
        var request = new RegisterRequest(
            "Nguyen Van A",
            "123456789",
            "0987654321",
            "admin@example.com",
            "2000-05-24",
            "123 Street",
            "school.edu.vn",
            "School Name",
            "456 School Street",
            "700000",
            "Principal",
            500
        );

        var expectedCommand = new RegisterCommand(
            request.contactFullName(),
            request.identityNumber(),
            request.contactPhone(),
            request.contactEmail(),
            java.time.LocalDate.of(2000, 5, 24),
            request.contactAddress(),
            request.schoolDomain(),
            request.schoolName(),
            request.schoolAddress(),
            request.postalCode(),
            request.position(),
            request.studentCount()
        );

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        verify(registerUseCase).execute(expectedCommand);
    }
}
