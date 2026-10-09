package com.car.rental.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Only one ZK operation (enroll / verify / delete) at a time.
 * The physical terminal cannot safely serve two long sessions in parallel.
 * Acquisition is timed so callers do not wait forever when the device is busy.
 */
@Component
public class FingerprintDeviceGate {

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Acquire the gate with unbounded wait (legacy). Prefer {@link #call(long, Callable)}.
     */
    public <T> T call(Callable<T> action) throws Exception {
        lock.lock();
        try {
            return action.call();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquire the gate within {@code timeoutMs}. Throws {@link FingerprintDeviceBusyException}
     * if the lock cannot be obtained in time.
     */
    public <T> T call(long timeoutMs, Callable<T> action) throws Exception {
        long wait = timeoutMs <= 0 ? 1 : timeoutMs;
        boolean acquired = lock.tryLock(wait, TimeUnit.MILLISECONDS);
        if (!acquired) {
            throw new FingerprintDeviceBusyException();
        }
        try {
            return action.call();
        } finally {
            lock.unlock();
        }
    }

    public void run(GateRunnable action) throws Exception {
        call(() -> {
            action.run();
            return null;
        });
    }

    public void run(long timeoutMs, GateRunnable action) throws Exception {
        call(timeoutMs, () -> {
            action.run();
            return null;
        });
    }

    /** True if another thread currently holds the gate. */
    public boolean isBusy() {
        return lock.isLocked();
    }

    @FunctionalInterface
    public interface GateRunnable {
        void run() throws Exception;
    }
}
