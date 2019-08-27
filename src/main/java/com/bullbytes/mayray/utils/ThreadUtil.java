package com.bullbytes.mayray.utils;


import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Helps with {@link Thread}s.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ThreadUtil {
    ;

    /**
     * Creates an {@link ExecutorService} that keeps its threads alive for reuse.
     *
     * @param maximumNumberOfThreads the maximum number of threads the returned {@link ExecutorService} will create
     * @return an {@link ExecutorService} that keeps its threads alive for reuse
     */
    public static ExecutorService newCachedThreadPool(int maximumNumberOfThreads) {
        return new ThreadPoolExecutor(0, maximumNumberOfThreads,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
    }

    /**
     * Gets a {@link Thread} by its ID.
     *
     * @param threadId the ID of the thread
     * @return the {@link Thread} wrapped in an {@link Optional} in case it wasn't found
     */
    public static Optional<Thread> getThread(long threadId) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getId() == threadId)
                .findFirst();
    }
}
