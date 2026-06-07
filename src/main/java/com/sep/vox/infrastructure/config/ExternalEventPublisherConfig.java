package com.sep.vox.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.infrastructure.event.publisher.NoOpExternalEventPublisher;

@Configuration
public class ExternalEventPublisherConfig {

    @Bean
    @ConditionalOnMissingBean(ExternalEventPublisherPort.class)
    public ExternalEventPublisherPort noOpExternalEventPublisher() {
        return new NoOpExternalEventPublisher();
    }
}
