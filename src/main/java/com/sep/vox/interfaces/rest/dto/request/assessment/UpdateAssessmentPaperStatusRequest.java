package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateAssessmentPaperStatusCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentPaperStatusRequest(
    @NotBlank(message = "Action là bắt buộc")
    @Pattern(
        regexp = "SUBMIT|APPROVE|REQUEST_REVISION|LOCK|REOPEN",
        message = "Action không hợp lệ"
    )
    String action,
    String note
) {

    public static UpdateAssessmentPaperStatusCommand toCommand(UUID paperId, UpdateAssessmentPaperStatusRequest request) {
        return new UpdateAssessmentPaperStatusCommand(paperId, request.action(), request.note());
    }
}
