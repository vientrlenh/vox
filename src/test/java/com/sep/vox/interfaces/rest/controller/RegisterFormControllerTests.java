package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.registration.ApproveRegisterFormUseCase;
import com.sep.vox.application.port.input.usecase.registration.RejectRegisterFormUseCase;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(RegisterFormController.class)
class RegisterFormControllerTests {

    private static final String REJECT_BODY = "{\"reason\": \"Missing documents\"}";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ApproveRegisterFormUseCase approveRegisterFormUseCase;
    @MockitoBean
    private RejectRegisterFormUseCase rejectRegisterFormUseCase;

    private MockMvcTester.MockMvcRequestBuilder rejectRequest(UUID id) {
        return mvc.post().uri("/api/v1/register-forms/{id}/reject", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(REJECT_BODY);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void reject_should_return_ok_response_for_system_admin() {
        var id = UUID.randomUUID();

        var result = rejectRequest(id).exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Đơn đăng ký đã từ chối thành công");
        verify(rejectRegisterFormUseCase).execute(new RejectRegisterFormCommand(id, "Missing documents"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void reject_should_return_bad_request_when_reason_is_blank() {
        var result = mvc.post().uri("/api/v1/register-forms/{id}/reject", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\": \"\"}")
            .exchange();

        assertThat(result).hasStatus(400);
        verifyNoInteractions(rejectRegisterFormUseCase);
    }

    @Test
    void reject_should_return_401_without_authenticated_user() {
        var result = rejectRequest(UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(401);
        verifyNoInteractions(rejectRegisterFormUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void reject_should_return_403_for_role_other_than_system_admin() {
        var result = rejectRequest(UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(rejectRegisterFormUseCase);
    }

    /**
     * The body is valid on purpose: {@code @Valid} runs before {@code @PreAuthorize}, so an invalid body
     * would be answered with 400 and never reach the role check.
     */
    @Test
    @WithMockUser(roles = "TEACHER")
    void approve_should_return_403_for_role_other_than_system_admin() {
        var id = UUID.randomUUID();
        var result = mvc.post().uri("/api/v1/register-forms/{id}/approve", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"registerFormId\": \"" + id + "\", \"schoolCode\": \"VOX_HCM\", \"provinceCode\": \"79\"}")
            .exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(approveRegisterFormUseCase);
    }
}
