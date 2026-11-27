package com.sep.vox.infrastructure.config;

import com.sep.vox.infrastructure.properties.InterestQuizProperties;
import com.sep.vox.infrastructure.properties.PracticeGenerationProperties;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Personalization configuration. Every number about the NUMBER OF QUESTIONS is declared in application.yaml under
 * {@code app.personalization.*}, with no constants scattered in code -- see the two properties classes below.
 *
 * <p>Registered explicitly even though {@code VoxApplication} already has {@code @ConfigurationPropertiesScan}:
 * the repo's three existing properties classes (S3, outbox, SePay) all do so. Relying on the scan alone means the day
 * someone removes that annotation, Spring reports "not registered as a bean" at STARTUP -- and
 * compileJava cannot catch that kind of error.
 */
@Configuration
@EnableConfigurationProperties({
    PracticeGenerationProperties.class,
    InterestQuizProperties.class
})
public class PersonalizationConfig {
}
