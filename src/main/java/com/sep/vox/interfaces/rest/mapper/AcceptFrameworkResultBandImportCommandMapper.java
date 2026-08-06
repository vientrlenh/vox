package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptFrameworkResultBandImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptFrameworkResultBandImportCommandMapper {

    public static AcceptFrameworkResultBandImportCommand fromRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptFrameworkResultBandImportCommand(sessionId, request.confirmedMapping());
    }
}
