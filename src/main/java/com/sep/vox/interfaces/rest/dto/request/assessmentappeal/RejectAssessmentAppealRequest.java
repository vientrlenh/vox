package com.sep.vox.interfaces.rest.dto.request.assessmentappeal;

import com.sep.vox.application.port.input.command.assessmentappeal.RejectAssessmentAppealCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAssessmentAppealRequest(
    @NotBlank(message = "The reason for rejecting the appeal must be stated")
    @Size(max = 512, message = "Rejection reason must not exceed 512 characters")
    String reason
) {

    public static RejectAssessmentAppealCommand toCommand(UUID appealId, RejectAssessmentAppealRequest request) {
        return new RejectAssessmentAppealCommand(appealId, request.reason());
    }
}
