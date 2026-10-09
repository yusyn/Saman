package com.car.rental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fingerprint")
public class FingerprintProperties {

    private static final int MIN_TIMEOUT_MS = 200;
    private static final int MAX_TIMEOUT_MS = 60_000;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 2500;
    private static final int DEFAULT_HANDSHAKE_TIMEOUT_MS = 2500;
    private static final int DEFAULT_GATE_ACQUIRE_TIMEOUT_MS = 3000;

    private String host = "192.168.20.10";
    private int port = 4370;
    private boolean mock = false;
    /** TCP connect timeout for preflight and connect (ms). */
    private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    /** SO timeout used while waiting for ZK handshake reply during preflight/connect (ms). */
    private int handshakeTimeoutMs = DEFAULT_HANDSHAKE_TIMEOUT_MS;
    /** Max wait to acquire the device gate before failing with "device busy" (ms). */
    private int gateAcquireTimeoutMs = DEFAULT_GATE_ACQUIRE_TIMEOUT_MS;
    private int verifyTimeoutSeconds = 40;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public int getConnectTimeoutMs() {
        return sanitizeTimeoutMs(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS);
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getHandshakeTimeoutMs() {
        return sanitizeTimeoutMs(handshakeTimeoutMs, DEFAULT_HANDSHAKE_TIMEOUT_MS);
    }

    public void setHandshakeTimeoutMs(int handshakeTimeoutMs) {
        this.handshakeTimeoutMs = handshakeTimeoutMs;
    }

    public int getGateAcquireTimeoutMs() {
        return sanitizeTimeoutMs(gateAcquireTimeoutMs, DEFAULT_GATE_ACQUIRE_TIMEOUT_MS);
    }

    public void setGateAcquireTimeoutMs(int gateAcquireTimeoutMs) {
        this.gateAcquireTimeoutMs = gateAcquireTimeoutMs;
    }

    public int getVerifyTimeoutSeconds() {
        if (verifyTimeoutSeconds <= 0) {
            return 40;
        }
        return Math.min(verifyTimeoutSeconds, 300);
    }

    public void setVerifyTimeoutSeconds(int verifyTimeoutSeconds) {
        this.verifyTimeoutSeconds = verifyTimeoutSeconds;
    }

    /**
     * Reject non-positive or absurd values so misconfiguration cannot cause unbounded waits.
     */
    public static int sanitizeTimeoutMs(int value, int defaultMs) {
        if (value < MIN_TIMEOUT_MS || value > MAX_TIMEOUT_MS) {
            return defaultMs;
        }
        return value;
    }
}
