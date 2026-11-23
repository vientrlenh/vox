package com.sep.vox.interfaces.rest.dto.request.assessmentappeal;

import com.sep.vox.application.port.input.command.assessmentappeal.ApproveAssessmentAppealCommand;
import java.util.UUID;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record ApproveAssessmentAppealRequest(
    @NotNull(message = "Phải đặt hạn xử lý cho đơn phúc khảo")
    Instant deadline
) {

    public static ApproveAssessmentAppealCommand toCommand(UUID appealId, ApproveAssessmentAppealRequest request) {
        return new ApproveAssessmentAppealCommand(appealId, request.deadline());
    }
}
