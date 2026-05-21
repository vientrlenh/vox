package com.sep.vox.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    private static final int MAIL_CORE_POOL_SIZE = 2;
    private static final int MAIL_MAX_POOL_SIZE = 5;
    private static final int MAIL_QUEUE_CAPACITY = 100;
    private static final String MAIL_THREAD_NAME_PREFIX = "mail-";


    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAIL_CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAIL_MAX_POOL_SIZE);
        executor.setQueueCapacity(MAIL_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(MAIL_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }
}
