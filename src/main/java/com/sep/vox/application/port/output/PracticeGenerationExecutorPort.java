package com.sep.vox.application.port.output;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Runs practice-generation work on a background thread that carries the caller's security context.
 */
public interface PracticeGenerationExecutorPort {

    /**
     * Schedules the work and returns immediately. An exception thrown by the work is logged and never reaches the
     * caller; when the queue is full the work runs on the calling thread instead of being rejected.
     */
    void runAsync(Runnable work);

    /**
     * Schedules the work and returns its future. An exception thrown by the work completes the future exceptionally.
     */
    <T> CompletableFuture<T> supplyAsync(Supplier<T> work);
}
