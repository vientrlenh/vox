package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.InterestQuizConfigPort;


@ConfigurationProperties(prefix = "app.personalization.quiz")
public record InterestQuizProperties(
    Integer itemCount
) implements InterestQuizConfigPort {
    public InterestQuizProperties {
        itemCount = itemCount == null ? 7 : itemCount;
    }
}
