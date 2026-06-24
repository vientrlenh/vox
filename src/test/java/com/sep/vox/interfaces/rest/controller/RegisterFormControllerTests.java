package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.registration.ApproveRegisterFormUseCase;
import com.sep.vox.application.port.input.usecase.registration.RejectRegisterFormUseCase;
import com.sep.vox.interfaces.rest.dto.request.RejectRegisterFormRequest;

public class RegisterFormControllerTests {

    @Test
    void reject_should_return_ok_response() {
        var approveRegisterFormUseCase = mock(ApproveRegisterFormUseCase.class);
        var rejectRegisterFormUseCase = mock(RejectRegisterFormUseCase.class);
        var controller = new RegisterFormController(approveRegisterFormUseCase, rejectRegisterFormUseCase);
        var id = UUID.randomUUID();
        var request = new RejectRegisterFormRequest("Missing documents");
        var expectedCommand = new RejectRegisterFormCommand(id, request.reason());

        var response = controller.reject(id, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Đơn đăng ký đã từ chối thành công");
        assertThat(response.getBody().data()).isNull();
        verify(rejectRegisterFormUseCase).execute(expectedCommand);
    }
}
