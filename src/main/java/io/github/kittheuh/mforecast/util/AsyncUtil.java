package io.github.kittheuh.mforecast.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

// ported from PolyBot#util/AsyncUtil.java
public class AsyncUtil {
    private static final Logger LOGGER = Logger.getLogger(AsyncUtil.class.getName());
    public static final ExecutorService SERVICE = Executors.newFixedThreadPool(2, r -> nextThread("AU-Regular", r));
    public static final ScheduledExecutorService SCHEDULED_SERVICE =
            Executors.newScheduledThreadPool(1, r -> nextThread("AU-Scheduler", r));

    private static final Map<String, ScheduledFuture<?>> DELAYED_FUTURES = new HashMap<>();
    private static final AtomicInteger threadNumber = new AtomicInteger();

    private static synchronized Thread nextThread(String prefix, Runnable r) {
        String name = prefix + "/" + threadNumber.getAndIncrement();
        Thread thread = new Thread(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "AsyncUtil thread \"%s\" caught an uncaught exception".formatted(name), t);
            }
        }, name);
        thread.setDaemon(true);
        return thread;
    }

    public static void shutdown(boolean runAllPending) {
        SERVICE.shutdown();
        SCHEDULED_SERVICE.shutdown();

        try {
            SERVICE.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
        try {
            SCHEDULED_SERVICE.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
        var l = SERVICE.shutdownNow();
        var l2 = SCHEDULED_SERVICE.shutdownNow();

        if (runAllPending) {
            l.forEach(Runnable::run);
            l2.forEach(Runnable::run);
        }
    }

    public static void scheduleDelayable(String id, Runnable r, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = DELAYED_FUTURES.get(id);
        if (future != null && !future.isDone()) future.cancel(true);

        future = SCHEDULED_SERVICE.schedule(() -> {
            DELAYED_FUTURES.remove(id);
            r.run();
        }, delay, unit);
        DELAYED_FUTURES.put(id, future);
    }

    public static void run(final Runnable runnable) {
        SERVICE.execute(runnable);
    }

    /**
     * Run an async task that will be stopped if it exceeds the given timeout
     * @param runnable The task to run
     * @param timeout Max time to run task for
     * @param unit Time unit to use for timeout
     * @param timeoutRunnable A task to run if the timeout runs out
     */
    public static void timeoutRun(final Runnable runnable, final long timeout, final TimeUnit unit, final Runnable timeoutRunnable) {
        run(() -> {
            Thread thread = Thread.currentThread();

            ScheduledFuture<?> scheduledFuture = SCHEDULED_SERVICE.schedule(() -> {
                LOGGER.warning("AsyncUtil thread \"%s\" was interrupted after %s %s timeout!".formatted(thread.getName(), timeout, unit.name().toLowerCase()));
                thread.interrupt();
                if (timeoutRunnable != null) timeoutRunnable.run();
            }, timeout, unit);

            runnable.run();
            scheduledFuture.cancel(true);
        });
    }

    public static void schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        SCHEDULED_SERVICE.schedule(runnable, delay, unit);
    }
}
