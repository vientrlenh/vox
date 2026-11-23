package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintVersionStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentBlueprintVersionStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(regexp = "PUBLISH|ARCHIVE", message = "Action không hợp lệ")
    String action,
    String note
) {

    public static UpdateAssessmentBlueprintVersionStatusCommand toCommand(
            UUID versionId,
            UpdateAssessmentBlueprintVersionStatusRequest request) {
        return new UpdateAssessmentBlueprintVersionStatusCommand(versionId, request.action(), request.note());
    }
}
