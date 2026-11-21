package com.sep.vox.infrastructure.service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;

@Service
public class SpringPracticeGenerationExecutorService implements PracticeGenerationExecutorPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringPracticeGenerationExecutorService.class);

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
            LOGGER.error("Uncaught exception in practice generation background work", exception);
        }
    }
}
