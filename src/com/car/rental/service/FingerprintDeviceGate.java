package com.car.rental.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Only one ZK operation (enroll / verify / delete) at a time.
 * The physical terminal cannot safely serve two long sessions in parallel.
 */
@Component
public class FingerprintDeviceGate {

    private final Object lock = new Object();

    public <T> T call(Callable<T> action) throws Exception {
        synchronized (lock) {
            return action.call();
        }
    }

    public void run(GateRunnable action) throws Exception {
        call(() -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    public interface GateRunnable {
        void run() throws Exception;
    }
}
