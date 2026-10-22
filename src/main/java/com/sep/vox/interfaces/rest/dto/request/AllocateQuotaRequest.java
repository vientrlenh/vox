package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AllocateAssessmentQuotaCommand;
import com.sep.vox.application.port.input.command.AllocatePracticeQuotaCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record AllocateQuotaRequest(
    @NotBlank(message = "Cách phân bổ không được để trống")
    @Pattern(regexp = "^(AUTO|MANUAL)$", message = "Cách phân bổ chỉ chấp nhận giá trị AUTO/MANUAL")
    String mode,

    @Valid
    List<AllocateUserQuotaAmountRequest> allocations,

    Boolean confirmWalletDraw
) {

    public static AllocatePracticeQuotaCommand toPracticeCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocatePracticeQuotaCommand(
            schoolId, 
            request.mode, 
            request.allocations.stream()
                .map(AllocateUserQuotaAmountRequest::toCommand)
                .toList(), 
            request.confirmWalletDraw
        );
    }

    public static AllocateAssessmentQuotaCommand toAssessmentCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocateAssessmentQuotaCommand(
            schoolId, 
            request.mode, 
            request.allocations.stream()
                .map(AllocateUserQuotaAmountRequest::toCommand)
                .toList()
        , request.confirmWalletDraw);
    }
}
