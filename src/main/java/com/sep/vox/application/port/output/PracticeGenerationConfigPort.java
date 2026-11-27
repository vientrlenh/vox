package com.sep.vox.application.port.output;

import java.time.Duration;

/**
 * Practice-paper generation parameters that the application needs to read. The real values are
 * bound from application.yaml ({@code app.personalization.generation.*}) in
 * PracticeGenerationProperties -- that record implements this interface, so the application does
 * not have to know about @ConfigurationProperties.
 */
public interface PracticeGenerationConfigPort {

    Integer paperTargetQuestionCount();

    Integer onlineCandidateCount();

    Duration onlineBudget();
}
