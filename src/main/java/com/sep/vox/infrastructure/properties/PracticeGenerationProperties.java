package com.sep.vox.infrastructure.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.PracticeGenerationConfigPort;

@ConfigurationProperties(prefix = "app.personalization.generation")
public record PracticeGenerationProperties(

    Integer paperTargetQuestionCount,

    Integer onlineCandidateCount,

    Duration onlineBudget
) implements PracticeGenerationConfigPort {
    public PracticeGenerationProperties {
        paperTargetQuestionCount = paperTargetQuestionCount == null
            ? 4
            : paperTargetQuestionCount;
        onlineCandidateCount = onlineCandidateCount == null
            ? 2
            : onlineCandidateCount;
        onlineBudget = onlineBudget == null
            ? Duration.ofSeconds(20)
            : onlineBudget;
    }
}
