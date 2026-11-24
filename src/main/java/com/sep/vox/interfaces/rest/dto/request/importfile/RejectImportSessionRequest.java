package com.sep.vox.interfaces.rest.dto.request.importfile;

import com.sep.vox.application.port.input.command.importfile.RejectImportSessionCommand;
import java.util.UUID;

public record RejectImportSessionRequest(
    String reason
) {

    public static RejectImportSessionCommand toCommand(UUID sessionId, RejectImportSessionRequest request) {
        return new RejectImportSessionCommand(sessionId, request == null ? null : request.reason());
    }
}
