package com.sep.vox.infrastructure.service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;

@Service
@Slf4j
public class SpringPracticeGenerationExecutorService implements PracticeGenerationExecutorPort {

    private final AsyncTaskExecutor practiceGenerationExecutor;

    public SpringPracticeGenerationExecutorService(
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.practiceGenerationExecutor = practiceGenerationExecutor;
    }

    @Override
    public void runAsync(Runnable work) {
        practiceGenerationExecutor.execute(() -> runAndLogFailure(work));
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> work) {
        return CompletableFuture.supplyAsync(work, practiceGenerationExecutor);
    }

    private void runAndLogFailure(Runnable work) {
        try {
            work.run();
        } catch (RuntimeException exception) {
            log.error("Uncaught exception in practice generation background work", exception);
        }
    }
}
