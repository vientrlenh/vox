package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptSchoolAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.AcceptSystemAssessmentPolicyImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptAssessmentPolicyImportCommandMapper {

    public static AcceptSystemAssessmentPolicyImportCommand fromSystemRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptSystemAssessmentPolicyImportCommand(sessionId, request.confirmedMapping());
    }

    public static AcceptSchoolAssessmentPolicyImportCommand fromSchoolRequest(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptSchoolAssessmentPolicyImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
