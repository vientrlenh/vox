package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateExamDeliveryModeCommand;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamDeliveryModeRequest;

public final class UpdateExamDeliveryModeCommandMapper {

    private UpdateExamDeliveryModeCommandMapper() {
    }

    public static UpdateExamDeliveryModeCommand fromRequest(UUID examId, UpdateExamDeliveryModeRequest request) {
        return new UpdateExamDeliveryModeCommand(examId, request.deliveryMode());
    }
}
