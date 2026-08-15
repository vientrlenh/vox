package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

class AsyncConfigTests {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void should_keep_file_import_executor_aborting_so_dispatcher_can_requeue() {
        var executor = config.fileImportExecutor();

        try {
            // ImportJobDispatcher dựa vào việc bị từ chối để giữ job lại cho sweeper requeue,
            // nên pool này không được chạy job trên chính thread scheduler.
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
        } finally {
            executor.shutdown();
        }
    }
}
