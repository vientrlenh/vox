package com.sep.vox.application.port.input.command.order;

import java.util.UUID;

public record CancelOrderCommand(
    UUID orderId
) {
    
}
