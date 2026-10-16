package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.port.input.command.ApproveAssessmentAppealCommand;

import jakarta.validation.constraints.NotNull;

public record ApproveAssessmentAppealRequest(
    @NotNull(message = "Phải đặt hạn xử lý cho đơn phúc khảo")
    Instant deadline
) {

    public static ApproveAssessmentAppealCommand toCommand(UUID appealId, ApproveAssessmentAppealRequest request) {
        return new ApproveAssessmentAppealCommand(
            appealId,
            request.deadline()
        );
    }
}
