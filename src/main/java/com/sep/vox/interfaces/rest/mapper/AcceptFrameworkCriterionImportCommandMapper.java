package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptFrameworkCriterionImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptFrameworkCriterionImportCommandMapper {

    public static AcceptFrameworkCriterionImportCommand fromRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptFrameworkCriterionImportCommand(sessionId, request.confirmedMapping());
    }
}
