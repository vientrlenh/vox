package com.sep.vox.interfaces.rest.dto.request.assessmentappeal;

import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import java.util.UUID;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record ApproveAssessmentAppealRequest(
    @NotNull(message = "A processing deadline must be set for the appeal")
    Instant deadline
) {

    public static ApproveAssessmentAppealCommand toCommand(UUID appealId, ApproveAssessmentAppealRequest request) {
        return new ApproveAssessmentAppealCommand(appealId, request.deadline());
    }
}
