package com.sep.vox.infrastructure.config;

import java.util.concurrent.ThreadPoolExecutor;

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
    // Import hàng loạt user sinh một mail thiết lập mật khẩu cho mỗi dòng, nên queue phải
    // đủ cho một file import bình thường.
    private static final int MAIL_QUEUE_CAPACITY = 1000;
    private static final String MAIL_THREAD_NAME_PREFIX = "mail-";

    private static final int FILE_CORE_POOL_SIZE = 2;
    private static final int FILE_MAX_POOL_SIZE = 2;
    private static final int FILE_QUEUE_CAPACITY = 0;
    private static final String FILE_THREAD_NAME_PREFIX = "file-import-";


    @Bean(name = "mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAIL_CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAIL_MAX_POOL_SIZE);
        executor.setQueueCapacity(MAIL_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(MAIL_THREAD_NAME_PREFIX);
        // Queue đầy thì gửi ngay trên caller thread (backpressure) thay vì để AbortPolicy
        // mặc định ném RejectedExecutionException vào giữa tiến trình đang gọi — đúng ca
        // import hàng loạt user, nơi một mail bị từ chối từng làm chết cả phiên import.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

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
}
