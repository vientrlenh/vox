package com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem;

import com.sep.vox.application.port.input.command.assessmentpolicyschool.AcceptSchoolAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.AcceptSystemAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.rubricschool.AcceptSchoolRubricVersionImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricCriterionImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricResultBandImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptSystemRubricVersionImportCommand;
import java.util.UUID;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/**
 * Body of the request that confirms the column mapping, shared by the new import flows.
 *
 * <p>The key is the column title in the file, the value is the system field name.
 * {@code AcceptQuestionImportRequest} has exactly the same content but is kept as it is -- the parameter type
 * of a running endpoint is not changed just to merge names.
 */
public record AcceptImportRequest(
    @NotEmpty(message = "Mapping import không được để trống")
    Map<String, String> confirmedMapping
) {

    public static AcceptSystemAssessmentPolicyImportCommand toAcceptSystemAssessmentPolicyImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptSystemAssessmentPolicyImportCommand(sessionId, request.confirmedMapping());
    }

    public static AcceptSchoolAssessmentPolicyImportCommand toAcceptSchoolAssessmentPolicyImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptSchoolAssessmentPolicyImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

    //  Cho System Admin
    public static AcceptRubricCriterionImportCommand toAcceptRubricCriterionImportCommand(UUID sessionId,  AcceptImportRequest request) {
        return new AcceptRubricCriterionImportCommand(null, sessionId, request.confirmedMapping());
    }

    // For School Admin (prepared in advance)
    public static AcceptRubricCriterionImportCommand toAcceptRubricCriterionImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricCriterionImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

    // For System Admin
    public static AcceptRubricResultBandImportCommand toAcceptRubricResultBandImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricResultBandImportCommand(null, sessionId, request.confirmedMapping());
    }

    // For School Admin
    public static AcceptRubricResultBandImportCommand toAcceptRubricResultBandImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptRubricResultBandImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

    // 👉 Maps the Request DTO cleanly into the Command for System Admin
    public static AcceptSystemRubricVersionImportCommand toAcceptSystemRubricVersionImportCommand(UUID sessionId, AcceptImportRequest request) {
        return new AcceptSystemRubricVersionImportCommand(sessionId, request.confirmedMapping());
    }

    // Upgraded the method for School Admin as well, in case it is needed later
    public static AcceptSchoolRubricVersionImportCommand toAcceptSchoolRubricVersionImportCommand(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptSchoolRubricVersionImportCommand(schoolId, sessionId, request.confirmedMapping());
    }
}
