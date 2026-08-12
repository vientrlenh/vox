package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.personalization.quiz")
public record InterestQuizProperties(
    Integer itemCount
) {
    public InterestQuizProperties {
        itemCount = itemCount == null ? 7 : itemCount;
    }
}
