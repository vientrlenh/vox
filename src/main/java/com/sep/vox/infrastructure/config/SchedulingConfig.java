package com.sep.vox.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {
    
    @Bean
    ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder
            .poolSize(4)
            .threadNamePrefix("scheduling-")
            .awaitTermination(true)
            .awaitTerminationPeriod(Duration.ofSeconds(30))
            .build();
    }

    @Bean
    ThreadPoolTaskScheduler outboxTaskScheduler(ThreadPoolTaskSchedulerBuilder builder) {
        return builder
            .poolSize(2)
            .threadNamePrefix("outbox-")
            .awaitTermination(true)
            .awaitTerminationPeriod(Duration.ofSeconds(30))
            .build();
    }
}
