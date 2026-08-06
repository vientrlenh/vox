package com.sep.vox.infrastructure.config;

import com.sep.vox.application.config.PracticeGenerationProperties;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(PracticeGenerationProperties.class)
public class PersonalizationConfig {
}
