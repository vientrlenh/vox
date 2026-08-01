package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class AsyncConfigTests {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void should_run_mail_task_in_caller_thread_when_pool_and_queue_are_full() throws InterruptedException {
        var executor = config.mailExecutor();
        var gate = new CountDownLatch(1);
        var started = new CountDownLatch(executor.getMaxPoolSize());
        var executed = new AtomicInteger();

        try {
            // Bão hòa pool + queue: import hàng loạt user sinh ra rất nhiều mail cùng lúc.
            var saturating = executor.getMaxPoolSize() + executor.getQueueCapacity();
            for (var i = 0; i < saturating; i++) {
                executor.execute(() -> {
                    started.countDown();
                    awaitQuietly(gate);
                });
            }
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            // Task vượt ngưỡng phải chạy ngay trên caller thread, không được bị từ chối.
            assertThatCode(() -> executor.execute(executed::incrementAndGet)).doesNotThrowAnyException();
            assertThat(executed.get()).isEqualTo(1);
        } finally {
            gate.countDown();
            executor.shutdown();
        }
    }

    @Test
    void should_configure_mail_executor_with_caller_runs_rejection_policy() {
        var executor = config.mailExecutor();

        try {
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            executor.shutdown();
        }
    }

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

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
