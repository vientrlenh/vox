package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ClaimClassTestGradingCommand;
import com.sep.vox.interfaces.rest.dto.request.ClaimClassTestGradingRequest;

public final class ClaimClassTestGradingCommandMapper {

    private ClaimClassTestGradingCommandMapper() {
    }

    public static ClaimClassTestGradingCommand fromRequest(UUID assessmentId, ClaimClassTestGradingRequest request) {
        return new ClaimClassTestGradingCommand(
            assessmentId,
            request.roundType(),
            request.candidateResultIds()
        );
    }
}
