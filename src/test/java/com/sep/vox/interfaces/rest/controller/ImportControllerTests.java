package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import com.sep.vox.application.port.input.usecase.importfile.RejectImportSessionUseCase;
import com.sep.vox.application.response.input.importfile.RejectImportSessionResponse;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(ImportController.class)
class ImportControllerTests {

    private static final String REJECT_URI = "/api/v1/imports/{sessionId}/reject";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RejectImportSessionUseCase rejectImportSessionUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void rejectImportSession_should_return_reject_response() {
        var sessionId = UUID.randomUUID();
        var expected = new RejectImportSessionResponse(sessionId, "CANCELLED", "User cancelled");
        when(rejectImportSessionUseCase.execute(new RejectImportSessionCommand(sessionId, "User cancelled")))
            .thenReturn(expected);

        var result = mvc.post().uri(REJECT_URI, sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\": \"User cancelled\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Hủy import thành công");
        assertThat(result).bodyJson().extractingPath("$.data.reason").isEqualTo("User cancelled");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void rejectImportSession_should_accept_empty_body() {
        var sessionId = UUID.randomUUID();
        var command = new RejectImportSessionCommand(sessionId, null);
        when(rejectImportSessionUseCase.execute(command))
            .thenReturn(new RejectImportSessionResponse(sessionId, "CANCELLED", null));

        var result = mvc.post().uri(REJECT_URI, sessionId).exchange();

        assertThat(result).hasStatusOk();
        verify(rejectImportSessionUseCase).execute(command);
    }

    @Test
    void rejectImportSession_should_return_401_without_authenticated_user() {
        var result = mvc.post().uri(REJECT_URI, UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(401);
        verifyNoInteractions(rejectImportSessionUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void rejectImportSession_should_return_403_for_role_other_than_school_admin() {
        var result = mvc.post().uri(REJECT_URI, UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(rejectImportSessionUseCase);
    }
}
