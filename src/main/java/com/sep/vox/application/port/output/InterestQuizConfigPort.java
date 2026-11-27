package com.sep.vox.application.port.output;

/**
 * Interest quiz parameters that the application needs to read. The real values are bound from
 * application.yaml ({@code app.personalization.quiz.*}) in InterestQuizProperties.
 */
public interface InterestQuizConfigPort {

    Integer itemCount();
}
