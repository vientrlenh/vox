package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptFrameworkVersionImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptFrameworkVersionImportCommandMapper {

    public static AcceptFrameworkVersionImportCommand fromRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptFrameworkVersionImportCommand(sessionId, request.confirmedMapping());
    }
}
