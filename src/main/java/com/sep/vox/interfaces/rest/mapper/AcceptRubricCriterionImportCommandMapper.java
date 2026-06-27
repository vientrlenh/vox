package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptRubricCriterionImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptRubricCriterionImportCommandMapper {

    //  Cho System Admin
    public static AcceptRubricCriterionImportCommand fromSystemRequest(UUID sessionId,  AcceptImportRequest request) {
        return new AcceptRubricCriterionImportCommand(null, sessionId, request.confirmedMapping());
    }

    //  Cho School Admin (Chuẩn bị sẵn)
    public static AcceptRubricCriterionImportCommand fromSchoolRequest(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricCriterionImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}