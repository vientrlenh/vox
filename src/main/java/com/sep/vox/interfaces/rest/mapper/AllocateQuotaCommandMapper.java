package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AllocateExamQuotaCommand;
import com.sep.vox.application.port.input.command.AllocatePracticeQuotaCommand;
import com.sep.vox.application.port.input.command.AllocateUserQuotaAmountCommand;
import com.sep.vox.interfaces.rest.dto.request.AllocateQuotaRequest;

public final class AllocateQuotaCommandMapper {

    private AllocateQuotaCommandMapper() {
    }

    public static AllocateExamQuotaCommand toExamCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocateExamQuotaCommand(
            schoolId, request.mode(), toAllocations(request), confirmWalletDraw(request));
    }

    public static AllocatePracticeQuotaCommand toPracticeCommand(UUID schoolId, AllocateQuotaRequest request) {
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
