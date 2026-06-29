package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptRubricResultBandImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptRubricResultBandImportCommandMapper {

    // Dành cho System Admin
    public static AcceptRubricResultBandImportCommand fromSystemRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricResultBandImportCommand(null, sessionId, request.confirmedMapping());
    }

    // Dành cho School Admin
    public static AcceptRubricResultBandImportCommand fromSchoolRequest(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricResultBandImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}