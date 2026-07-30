package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AllocateClassTestQuotaCommand;
import com.sep.vox.application.port.input.command.AllocatePracticeQuotaCommand;
import com.sep.vox.application.port.input.command.UserQuotaAmount;
import com.sep.vox.interfaces.rest.dto.request.AllocateQuotaRequest;

public final class AllocateQuotaCommandMapper {

    private AllocateQuotaCommandMapper() {
    }

    public static AllocateClassTestQuotaCommand toClassTestCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocateClassTestQuotaCommand(schoolId, request.mode(), toAllocations(request));
    }

    public static AllocatePracticeQuotaCommand toPracticeCommand(UUID schoolId, AllocateQuotaRequest request) {
        return new AllocatePracticeQuotaCommand(schoolId, request.mode(), toAllocations(request));
    }

    private static List<UserQuotaAmount> toAllocations(AllocateQuotaRequest request) {
        if (request.allocations() == null) {
            return List.of();
        }
        return request.allocations().stream()
            .map(item -> new UserQuotaAmount(item.userId(), item.amount()))
            .toList();
    }
}
