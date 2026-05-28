package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.systemadmin.ApproveRegisterFormUseCase;
import com.sep.vox.application.port.input.usecase.systemadmin.RejectRegisterFormUseCase;
import com.sep.vox.interfaces.rest.dto.request.ApproveRegisterFormRequest;
import com.sep.vox.interfaces.rest.dto.request.RejectRegisterFormRequest;

public class RegisterFormControllerTests {

    @Test
    void approve_should_return_ok_response() {
        var approveRegisterFormUseCase = mock(ApproveRegisterFormUseCase.class);
        var rejectRegisterFormUseCase = mock(RejectRegisterFormUseCase.class);
        var controller = new RegisterFormController(approveRegisterFormUseCase, rejectRegisterFormUseCase);
        var id = UUID.randomUUID();
        var request = new ApproveRegisterFormRequest(
            "VOX_SCHOOL",
            "Vox School",
            "AI oral exam evaluation school",
            "0987654321",
            "admin@vox.edu.vn",
            "vox.edu.vn",
            "456 School Street",
            500,
            "Nguyen Van A",
            "2000-05-24",
            "123 Contact Street"
        );
        var expectedCommand = new ApproveRegisterFormCommand(
            id,
            request.schoolCode(),
            request.schoolName(),
            request.description(),
            request.contactPhone(),
            request.contactEmail(),
            request.schoolDomain(),
            request.schoolAddress(),
            request.studentCount(),
            request.contactFullName(),
            LocalDate.of(2000, 5, 24),
            request.contactAddress()
        );

        var response = controller.approve(id, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Đơn đăng ký đã được phê duyệt");
        assertThat(response.getBody().data()).isNull();
        verify(approveRegisterFormUseCase).execute(expectedCommand);
    }

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
