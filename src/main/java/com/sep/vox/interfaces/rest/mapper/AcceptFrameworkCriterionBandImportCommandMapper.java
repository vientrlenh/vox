package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptFrameworkCriterionBandImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptFrameworkCriterionBandImportCommandMapper {

    public static AcceptFrameworkCriterionBandImportCommand fromRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptFrameworkCriterionBandImportCommand(sessionId, request.confirmedMapping());
    }
}
