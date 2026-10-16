package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.util.UUID;

import com.sep.vox.application.port.input.command.RejectAssessmentAppealCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAssessmentAppealRequest(
    @NotBlank(message = "Phải nêu lý do từ chối đơn phúc khảo")
    @Size(max = 512, message = "Lý do từ chối tối đa 512 ký tự")
    String reason
) {

    public static RejectAssessmentAppealCommand toCommand(UUID appealId, RejectAssessmentAppealRequest request) {
        return new RejectAssessmentAppealCommand(appealId, request.reason());
    }
}
