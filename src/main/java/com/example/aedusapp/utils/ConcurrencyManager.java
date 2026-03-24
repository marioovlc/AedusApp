package com.example.aedusapp.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal Thread Pool Manager for AedusApp.
 * Re-uses threads instead of spawning 'new Thread().start()' constantly.
 */
public class ConcurrencyManager {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyManager.class);
    
    // Cached thread pool adjusts number of threads based on workload.
    // It creates new threads as needed, but will reuse previously constructed threads when they are available.
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Submits a Runnable task for execution.
     */
    public static void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * Submits a Callable/Task for execution returning a Future.
     * Often used implicitly when submitting javafx.concurrent.Task.
     */
    public static <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * Gracefully shutdown the thread pool (e.g., when the application exits).
     */
    public static void shutdown() {
        logger.info("Encerrando Hilos de ConcurrencyManager...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
