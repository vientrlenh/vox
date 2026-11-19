package com.sep.vox.infrastructure.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final int FILE_CORE_POOL_SIZE = 2;
    private static final int FILE_MAX_POOL_SIZE = 2;
    private static final int FILE_QUEUE_CAPACITY = 0;
    private static final String FILE_THREAD_NAME_PREFIX = "file-import-";

    private static final int PRACTICE_CORE_POOL_SIZE = 4;
    private static final int PRACTICE_MAX_POOL_SIZE = 8;
    private static final int PRACTICE_QUEUE_CAPACITY = 50;
    private static final String PRACTICE_THREAD_NAME_PREFIX = "practice-gen-";

    private static final int PUSH_CORE_POOL_SIZE = 2;
    private static final int PUSH_MAX_POOL_SIZE = 4;
    private static final int PUSH_QUEUE_CAPACITY = 20;
    private static final String PUSH_THREAD_NAME_PREFIX = "push-";

    /**
     * Keep the default {@code AbortPolicy}: {@code ImportJobDispatcher} relies on being rejected
     * to leave the job in {@code IMPORTING} for the sweeper to requeue. {@code CallerRunsPolicy} here would
     * run a whole import session on the scheduler thread and block the poll loop.
     */
    @Bean(name = "fileImportExecutor")
    public ThreadPoolTaskExecutor fileImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(FILE_CORE_POOL_SIZE);
        executor.setMaxPoolSize(FILE_MAX_POOL_SIZE);
        executor.setQueueCapacity(FILE_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(FILE_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }


    @Bean(name = "practiceGenerationExecutor")
    public AsyncTaskExecutor practiceGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PRACTICE_CORE_POOL_SIZE);
        executor.setMaxPoolSize(PRACTICE_MAX_POOL_SIZE);
        executor.setQueueCapacity(PRACTICE_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(PRACTICE_THREAD_NAME_PREFIX);
        // When the queue is full, run right away on the calling thread (slow but it still finishes) instead of throwing
        // RejectedExecutionException in the face of a student waiting to enter the session.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
    
    @Bean(name = "pushExecutor")
    ThreadPoolTaskExecutor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PUSH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(PUSH_MAX_POOL_SIZE);
        executor.setQueueCapacity(PUSH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(PUSH_THREAD_NAME_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
