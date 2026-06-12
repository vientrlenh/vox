package com.sep.vox.application.port.input.usecase.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.event.dummy.DummyReportGeneratedExternalEvent;
import com.sep.vox.application.port.input.command.dummy.PublishReportGeneratedCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;

@Service
public class PublishReportGeneratedUseCaseImpl implements IUseCase<PublishReportGeneratedCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(PublishReportGeneratedUseCaseImpl.class);

    private final ExternalEventPublisherPort externalEventPublisherPort;

    public PublishReportGeneratedUseCaseImpl(ExternalEventPublisherPort externalEventPublisherPort) {
        this.externalEventPublisherPort = externalEventPublisherPort;
    }

    @Override
    public Void execute(PublishReportGeneratedCommand input) {
        log.info("Publishing DummyReportGeneratedExternalEvent: reportId={}, requestedBy={}", input.reportId(), input.requestedBy());

        var event = new DummyReportGeneratedExternalEvent(input.reportId(), input.requestedBy(), input.status());
        externalEventPublisherPort.publish(event);

        return null;
    }
}
