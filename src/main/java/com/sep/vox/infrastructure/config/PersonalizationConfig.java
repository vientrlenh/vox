package com.sep.vox.infrastructure.config;

import com.sep.vox.application.config.PracticeGenerationProperties;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.sep.vox.application.port.input.service.WeaknessVectorSettings;

@Configuration
@EnableConfigurationProperties(PracticeGenerationProperties.class)
public class PersonalizationConfig {

    @Bean
    public WeaknessVectorSettings weaknessVectorSettings(
            @Value("${app.personalization.weakness.alpha:0.2}") double alpha,
            @Value("${app.personalization.weakness.shrinkage.pronunciation:3}") double pronunciationK,
            @Value("${app.personalization.weakness.shrinkage.fluency:4}") double fluencyK,
            @Value("${app.personalization.weakness.shrinkage.grammar:5}") double grammarK,
            @Value("${app.personalization.weakness.shrinkage.vocabulary:5}") double vocabularyK,
            @Value("${app.personalization.weakness.shrinkage.coherence:7}") double coherenceK,
            @Value("${app.personalization.weakness.reliable-observation-count:3}") int reliableCount,
            @Value("${app.personalization.weakness.minimum-criteria-per-evaluation:3}") int minimumCriteria,
            @Value("${app.personalization.weakness.minimum-class-prior-students:10}") int minimumClassStudents,
            @Value("${app.personalization.weakness.observation-window-days:60}") long observationWindowDays,
            @Value("${app.personalization.weakness.recent-window-days:14}") long recentWindowDays,
            @Value("${app.personalization.weakness.minimum-sub-attribute-frequency:3}") int minimumFrequency,
            @Value("${app.personalization.weakness.frequency-weight:0.6}") double frequencyWeight,
            @Value("${app.personalization.weakness.recent-frequency-weight:0.4}") double recentFrequencyWeight,
            @Value("${app.personalization.weakness.stale-after-hours:24}") long staleAfterHours,
            @Value("${app.personalization.weakness.batch-size:200}") int batchSize) {
        return new WeaknessVectorSettings(
            alpha,
            Map.of(
                "PRONUNCIATION", pronunciationK,
                "FLUENCY", fluencyK,
                "GRAMMAR", grammarK,
                "VOCABULARY", vocabularyK,
                "COHERENCE", coherenceK
            ),
            reliableCount,
            minimumCriteria,
            minimumClassStudents,
            Duration.ofDays(observationWindowDays),
            Duration.ofDays(recentWindowDays),
            minimumFrequency,
            frequencyWeight,
            recentFrequencyWeight,
            Duration.ofHours(staleAfterHours),
            Math.min(Math.max(batchSize, 1), 200)
        );
    }
}
