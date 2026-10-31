package io.github.andercmd.autto.core.cucumber;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Timer that aborts a scenario that runs for too long. A hung scenario (a browser that never answers, an endless
 * wait) would otherwise occupy a worker thread forever and block a parallel run.
 */
final class ScenarioWatchdog {

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "autto-scenario-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private ScenarioWatchdog() {
    }

    /** Runs {@code onTimeout} once {@code timeout} has elapsed, unless {@link Armed#disarm()} is called first. */
    static Armed arm(Duration timeout, Runnable onTimeout) {
        AtomicBoolean fired = new AtomicBoolean();
        ScheduledFuture<?> task = TIMER.schedule(() -> {
            fired.set(true);
            onTimeout.run();
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        return new Armed(task, fired);
    }

    /** A running guard. */
    static final class Armed {
        private final ScheduledFuture<?> task;
        private final AtomicBoolean fired;

        private Armed(ScheduledFuture<?> task, AtomicBoolean fired) {
            this.task = task;
            this.fired = fired;
        }

        /** Cancels the guard; returns whether it had already fired. */
        boolean disarm() {
            task.cancel(false);
            return fired.get();
        }
    }
}
