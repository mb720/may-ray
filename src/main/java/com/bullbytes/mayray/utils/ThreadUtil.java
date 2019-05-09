package com.bullbytes.mayray.utils;


import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.util.Optional;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.LinkedBlockingDeque;
        import java.util.concurrent.ThreadPoolExecutor;
        import java.util.concurrent.TimeUnit;

/**
 * Helps with {@link Thread}s.
 * <p>
 * Person of contact: Matthias Braun
 */
public enum ThreadUtil {
    ;
    private static final Logger log = LoggerFactory.getLogger(ThreadUtil.class);

    /**
     * @return an {@link ExecutorService} that assigns threads to task in a last in first out order. The
     * {@link ExecutorService} uses all available processors minus one, but at least one processor
     */
    static ExecutorService createLifoExecutor() {

        // Defines how many threads should process input at maximum. Have as many threads as there are processors minus
        // one, but at least one thread
        int maxPoolSize = Math.max(1, getProcessors() - 1);
        log.info("Maximum number of threads used: {}", maxPoolSize);

        // When the number of threads is greater than the number of cores, this is the maximum time that
        // excess idle threads will wait for new tasks before terminating
        long keepAliveTime = 3;
        var timeUnit = TimeUnit.SECONDS;

        // It's the stack that makes the executor assign threads to the submitted tasks in a last in, first out order
        return new ThreadPoolExecutor(0, maxPoolSize, keepAliveTime, timeUnit, new ThreadUtil.BlockingStack<>());
    }

    private static int getProcessors() {
        return Runtime.getRuntime().availableProcessors();
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

    /**
     * A stack that will block when it's full and clients try to add new elements to it.
     * Being a stack, it adds new elements in a last in first out manner: We put the most recently added element at the
     * first position in the stack.
     * <p>
     * If its capacity is unspecified, it defaults to {@link Integer#MAX_VALUE}.
     *
     * @param <E> the elements inside the {@link ThreadUtil.BlockingStack}
     */
    private static final class BlockingStack<E> extends LinkedBlockingDeque<E> {

        @Override
        public  boolean offer(E e) {
            return offerFirst(e);
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
            return offerFirst(e, timeout, unit);
        }

        @Override
        public boolean add(E e) {
            return offerFirst(e);
        }

        @Override
        public void put(E e) throws InterruptedException {
            putFirst(e);
        }
    }
}
