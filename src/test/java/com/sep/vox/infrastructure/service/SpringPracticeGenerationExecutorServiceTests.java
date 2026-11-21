package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.infrastructure.config.AsyncConfig;

class SpringPracticeGenerationExecutorServiceTests {

    private static final long WAIT_SECONDS = 5;

    private AsyncTaskExecutor executor;
    private SpringPracticeGenerationExecutorService service;

    @BeforeEach
    void createService() {
        executor = new AsyncConfig().practiceGenerationExecutor();
        service = new SpringPracticeGenerationExecutorService(executor);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void runAsync_runsOnPracticeGenerationThreadWithCallerSecurityContext() throws InterruptedException {
        Authentication caller = new TestingAuthenticationToken("caller", "secret");
        SecurityContextHolder.getContext().setAuthentication(caller);
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicReference<Authentication> seenAuthentication = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);

        service.runAsync(() -> {
            threadName.set(Thread.currentThread().getName());
            seenAuthentication.set(SecurityContextHolder.getContext().getAuthentication());
            finished.countDown();
        });

        assertThat(finished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).startsWith("practice-gen-");
        assertThat(seenAuthentication.get()).isSameAs(caller);
    }

    @Test
    void runAsync_doesNotLetAnExceptionEscapeAndKeepsAcceptingWork() throws InterruptedException {
        CountDownLatch failingWorkStarted = new CountDownLatch(1);
        CountDownLatch laterWorkFinished = new CountDownLatch(1);

        service.runAsync(() -> {
            failingWorkStarted.countDown();
            throw new IllegalStateException("boom");
        });
        service.runAsync(laterWorkFinished::countDown);

        assertThat(failingWorkStarted.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(laterWorkFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void supplyAsync_returnsTheValueComputedOnPracticeGenerationThread() throws Exception {
        CompletableFuture<String> future = service.supplyAsync(() -> Thread.currentThread().getName());

        assertThat(future.get(WAIT_SECONDS, TimeUnit.SECONDS)).startsWith("practice-gen-");
    }

    @Test
    void supplyAsync_completesExceptionallyWhenTheWorkThrows() {
        CompletableFuture<String> future = service.supplyAsync(() -> {
            throw new IllegalStateException("boom");
        });

        assertThat(future).failsWithin(WAIT_SECONDS, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(IllegalStateException.class);
    }
}
