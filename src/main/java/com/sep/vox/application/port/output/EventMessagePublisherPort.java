package com.sep.vox.application.port.output;

import java.util.concurrent.CompletableFuture;

import com.sep.vox.domain.model.outbox.Outbox;

public interface EventMessagePublisherPort {
    CompletableFuture<Void> publish(Outbox event);
}
