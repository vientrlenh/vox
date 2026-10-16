package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.RejectImportSessionCommand;

public record RejectImportSessionRequest(
    String reason
) {

    public static RejectImportSessionCommand toCommand(UUID importSessionId, RejectImportSessionRequest request) {
        return new RejectImportSessionCommand(importSessionId, request.reason);
    }
}
