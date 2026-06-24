package com.sep.vox.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    
    private static final int MAIL_CORE_POOL_SIZE = 2;
    private static final int MAIL_MAX_POOL_SIZE = 5;
    private static final int MAIL_QUEUE_CAPACITY = 100;
    private static final String MAIL_THREAD_NAME_PREFIX = "mail-";

    private static final int FILE_CORE_POOL_SIZE = 2;
    private static final int FILE_MAX_POOL_SIZE = 2;
    private static final int FILE_QUEUE_CAPACITY = 0;
    private static final String FILE_THREAD_NAME_PREFIX = "file-import-";


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

    @Bean(name = "fileImportExecutor")
    public ThreadPoolTaskExecutor fileImportExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(FILE_CORE_POOL_SIZE);
        executor.setMaxPoolSize(FILE_MAX_POOL_SIZE);
        executor.setQueueCapacity(FILE_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(FILE_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }
}
