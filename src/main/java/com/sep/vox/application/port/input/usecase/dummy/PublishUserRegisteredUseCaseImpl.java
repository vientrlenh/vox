package com.sep.vox.application.port.input.usecase.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.event.dummy.DummyUserRegisteredExternalEvent;
import com.sep.vox.application.port.input.command.dummy.PublishUserRegisteredCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;

@Service
public class PublishUserRegisteredUseCaseImpl implements IUseCase<PublishUserRegisteredCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(PublishUserRegisteredUseCaseImpl.class);

    private final ExternalEventPublisherPort externalEventPublisherPort;

    public PublishUserRegisteredUseCaseImpl(ExternalEventPublisherPort externalEventPublisherPort) {
        this.externalEventPublisherPort = externalEventPublisherPort;
    }

    @Override
    public Void execute(PublishUserRegisteredCommand input) {
        log.info("Publishing DummyUserRegisteredExternalEvent: userId={}, email={}", input.userId(), input.email());

        var event = new DummyUserRegisteredExternalEvent(input.userId(), input.email(), input.fullName());
        externalEventPublisherPort.publish(event);

        return null;
    }
}
