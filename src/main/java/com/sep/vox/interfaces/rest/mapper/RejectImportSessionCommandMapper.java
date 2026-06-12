package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import com.sep.vox.interfaces.rest.dto.request.RejectImportSessionRequest;

public final class RejectImportSessionCommandMapper {

    private RejectImportSessionCommandMapper() {
    }

    public static RejectImportSessionCommand fromRequest(UUID sessionId, RejectImportSessionRequest request) {
        return new RejectImportSessionCommand(sessionId, request == null ? null : request.reason());
    }
}
