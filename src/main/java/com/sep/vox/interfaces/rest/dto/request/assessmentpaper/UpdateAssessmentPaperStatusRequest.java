package com.sep.vox.interfaces.rest.dto.request.assessmentpaper;

import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperStatusCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentPaperStatusRequest(
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REQUEST_REVISION|LOCK|REOPEN",
        message = "Action is invalid"
    )
    String action,
    String note
) {

    public static UpdateAssessmentPaperStatusCommand toCommand(UUID paperId, UpdateAssessmentPaperStatusRequest request) {
        return new UpdateAssessmentPaperStatusCommand(paperId, request.action(), request.note());
    }
}
