package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTests {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void should_keep_file_import_executor_aborting_so_dispatcher_can_requeue() {
        ThreadPoolTaskExecutor executor = config.fileImportExecutor();

        try {
            // ImportJobDispatcher relies on being rejected to keep the job for the sweeper to
            // requeue, so this pool must not run the job on the scheduler thread itself.
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
