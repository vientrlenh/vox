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
     * Giữ {@code AbortPolicy} mặc định: {@code ImportJobDispatcher} dựa vào việc bị từ chối
     * để giữ job ở {@code IMPORTING} cho sweeper requeue. {@code CallerRunsPolicy} ở đây sẽ
     * chạy cả phiên import trên thread scheduler và chặn luôn vòng poll.
     */
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


    @Bean(name = "practiceGenerationExecutor")
    public AsyncTaskExecutor practiceGenerationExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PRACTICE_CORE_POOL_SIZE);
        executor.setMaxPoolSize(PRACTICE_MAX_POOL_SIZE);
        executor.setQueueCapacity(PRACTICE_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(PRACTICE_THREAD_NAME_PREFIX);
        // Hàng đợi đầy thì chạy ngay trên thread gọi (chậm nhưng vẫn xong) thay vì ném
        // RejectedExecutionException vào mặt học sinh đang chờ vào phiên.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
    
    @Bean(name = "pushExecutor")
    ThreadPoolTaskExecutor pushExecutor() {
        var executor = new ThreadPoolTaskExecutor();
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
