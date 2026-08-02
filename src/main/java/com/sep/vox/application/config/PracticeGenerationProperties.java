package com.sep.vox.application.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.personalization.generation")
public record PracticeGenerationProperties(
    Integer paperTargetQuestionCount,
    Duration onlineBudget
) {
    public PracticeGenerationProperties {
        paperTargetQuestionCount = paperTargetQuestionCount == null
            ? 4
            : paperTargetQuestionCount;
        onlineBudget = onlineBudget == null
            ? Duration.ofSeconds(20)
            : onlineBudget;
    }
}
