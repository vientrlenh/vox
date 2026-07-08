package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamDeliveryModeCommand(
    UUID examId,
    String deliveryMode
) {
}
