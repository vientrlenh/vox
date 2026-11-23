package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.util.List;

import com.sep.vox.application.port.input.command.subscription.AllocateAssessmentQuotaCommand;
import com.sep.vox.application.port.input.command.subscription.AllocatePracticeQuotaCommand;
import com.sep.vox.application.port.input.command.subscription.AllocateUserQuotaAmountCommand;
import com.sep.vox.domain.common.DistributionMode;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public record AllocateQuotaRequest(
    @NotNull(message = "Cách phân bổ không được để trống")
    DistributionMode mode,

    // @NotEmpty is removed because auto mode does not need this list
    // @Valid is kept, to validate the fields inside the record if any appear
    @Valid
    List<AllocateUserQuotaAmountRequest> allocations,

    // Nullable: an old client that does not send this field must be treated as false in the mapper, not as a validation error.
    // true = the administrator has confirmed that the part beyond the pool will eat into the school's self-topped-up wallet (see
    // DistributeQuotaToUsersService.computeManualAmounts / the BusinessRuleViolationException thrown without confirmation).
    Boolean confirmWalletDraw
) {

    public static AllocateAssessmentQuotaCommand toAllocateAssessmentQuotaCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocateAssessmentQuotaCommand(
            schoolId, request.mode(), toAllocations(request), confirmWalletDraw(request));
    }

    public static AllocatePracticeQuotaCommand toAllocatePracticeQuotaCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocatePracticeQuotaCommand(
            schoolId, request.mode(), toAllocations(request), confirmWalletDraw(request));
    }

    private static boolean confirmWalletDraw(AllocateQuotaRequest request) {
        return Boolean.TRUE.equals(request.confirmWalletDraw());
    }

    private static List<AllocateUserQuotaAmountCommand> toAllocations(AllocateQuotaRequest request) {
        if (request.allocations() == null) {
            return List.of();
        }
        return request.allocations().stream()
            .map(item -> new AllocateUserQuotaAmountCommand(item.userId(), item.amountVnd()))
            .toList();
    }
}
