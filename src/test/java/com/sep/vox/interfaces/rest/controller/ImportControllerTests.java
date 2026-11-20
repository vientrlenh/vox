package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import com.sep.vox.application.port.input.usecase.importfile.RejectImportSessionUseCase;
import com.sep.vox.application.response.input.importfile.RejectImportSessionResponse;
import com.sep.vox.interfaces.rest.dto.request.RejectImportSessionRequest;

class ImportControllerTests {

    @Test
    void rejectImportSession_should_return_reject_response() {
        var rejectUseCase = mock(RejectImportSessionUseCase.class);
        var controller = new ImportController(rejectUseCase);
        var sessionId = UUID.randomUUID();
        var request = new RejectImportSessionRequest("User cancelled");
        var command = new RejectImportSessionCommand(sessionId, "User cancelled");
        var expected = new RejectImportSessionResponse(sessionId, "CANCELLED", "User cancelled");

        when(rejectUseCase.execute(command)).thenReturn(expected);

        var response = controller.rejectImportSession(sessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Hủy import thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(rejectUseCase).execute(command);
    }

    @Test
    void rejectImportSession_should_accept_empty_body() {
        var rejectUseCase = mock(RejectImportSessionUseCase.class);
        var controller = new ImportController(rejectUseCase);
        var sessionId = UUID.randomUUID();
        var command = new RejectImportSessionCommand(sessionId, null);
        var expected = new RejectImportSessionResponse(sessionId, "CANCELLED", null);

        when(rejectUseCase.execute(command)).thenReturn(expected);

        var response = controller.rejectImportSession(sessionId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(rejectUseCase).execute(command);
    }
}
