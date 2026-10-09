package com.car.rental.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Abstraction over the ZKTeco fingerprint terminal.
 * UI and business logic only talk to this interface.
 */
public interface FingerprintService {

    void connect() throws FingerprintException;

    void disconnect();

    boolean isConnected();

    /**
     * Real preflight: verifies the device is reachable and responds to ZK handshake.
     * Does not rely solely on {@link #isConnected()}.
     * <ul>
     *   <li>Mock: always succeeds (and marks connected).</li>
     *   <li>ZK: TCP connect + CMD_CONNECT handshake with bounded timeouts.</li>
     * </ul>
     * On success the connection may be kept open for a subsequent operation under the same gate.
     * Throws {@link FingerprintDeviceUnavailableException} if the device is down or silent.
     */
    void preflight() throws FingerprintException;

    /**
     * Start listening for the next successful verification (one-shot).
     * Implementations should use a short-lived connection and release the device
     * when verification finishes, times out, or is cancelled.
     */
    void listenForVerification(
            int timeoutSeconds,
            Consumer<VerificationResult> onVerified,
            Runnable onTimeout,
            Consumer<FingerprintException> onError
    );

    void cancelListen();

    /**
     * Cancel an in-progress enroll (register / enrollFingerOnly).
     * Default is no-op; implementations should flip their enrolling flag
     * so the blocking wait exits promptly.
     */
    default void cancelEnroll() {
    }

    List<DeviceUser> getUsers() throws FingerprintException;

    void createUser(String deviceUserId, String name) throws FingerprintException;

    void updateUserName(String deviceUserId, String name) throws FingerprintException;

    void deleteUser(String deviceUserId) throws FingerprintException;

    /**
     * Create user on device and enroll one finger (blocking until done or failed).
     * On enroll failure the implementation should roll back the device user when possible.
     */
    void registerUserWithFingerprint(String deviceUserId, String name, int fingerIndex)
            throws FingerprintException;

    /** Enroll an additional finger for an existing device user (blocking). */
    void enrollFingerOnly(String deviceUserId, int fingerIndex) throws FingerprintException;

    void startEnroll(String deviceUserId, int fingerIndex,
                     Consumer<EnrollResult> onFinished,
                     Consumer<FingerprintException> onError) throws FingerprintException;

    DeviceInfo getDeviceInfo() throws FingerprintException;
}
