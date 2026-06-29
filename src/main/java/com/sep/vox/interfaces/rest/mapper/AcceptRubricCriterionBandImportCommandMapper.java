package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptRubricCriterionBandImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptRubricCriterionBandImportCommandMapper {
    public static AcceptRubricCriterionBandImportCommand fromSystemRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricCriterionBandImportCommand(null, sessionId, request.confirmedMapping());
    }

    public static AcceptRubricCriterionBandImportCommand fromSchoolRequest(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricCriterionBandImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}