package com.car.rental.service;

import com.car.rental.service.zk.ZkConnection;
import com.car.rental.service.zk.ZkEventListener;
import com.car.rental.service.zk.ZkProtocol;
import com.car.rental.service.zk.ZkUserCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * ZKTeco fingerprint terminal adapter — orchestrates connection, user commands, and events.
 * Protocol details live under {@code com.car.rental.service.zk}.
 */
public class ZkFingerprintService implements FingerprintService {

    private final ZkConnection connection;
    private final ZkUserCommands users;
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private final AtomicBoolean enrolling = new AtomicBoolean(false);
    private final ZkEventListener events;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "zk-fingerprint-worker");
        t.setDaemon(true);
        return t;
    });
    private Future<?> listenTask;

    public ZkFingerprintService(String host, int port) {
        this(host, port, 8000);
    }

    public ZkFingerprintService(String host, int port, int connectTimeoutMs) {
        this.connection = new ZkConnection(host, port, connectTimeoutMs);
        this.users = new ZkUserCommands(connection);
        this.events = new ZkEventListener(connection, enrolling, listening);
    }

    public ZkFingerprintService() {
        this("192.168.20.200", 4370, 8000);
    }

    @Override
    public synchronized void connect() throws FingerprintException {
        connection.connect();
    }

    @Override
    public synchronized void disconnect() {
        cancelListen();
        cancelEnroll();
        connection.disconnect();
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void cancelListen() {
        listening.set(false);
        if (listenTask != null) {
            listenTask.cancel(true);
            listenTask = null;
        }
    }

    @Override
    public void cancelEnroll() {
        enrolling.set(false);
    }

    @Override
    public void listenForVerification(int timeoutSeconds,
                                      Consumer<VerificationResult> onVerified,
                                      Runnable onTimeout,
                                      Consumer<FingerprintException> onError) {
        cancelListen();
        listening.set(true);
        listenTask = executor.submit(() -> {
            try {
                if (!connection.isConnected()) {
                    connection.connect();
                }
                synchronized (ZkFingerprintService.this) {
                    connection.enableDeviceBestEffort("before verify");
                    try {
                        connection.sendCommand(ZkProtocol.CMD_STARTVERIFY, new byte[0]);
                    } catch (Exception ignored) {
                    }
                }
                events.listenUntilVerifiedOrTimeout(timeoutSeconds, onVerified, onTimeout);
            } catch (Exception e) {
                listening.set(false);
                if (onError != null) {
                    onError.accept(new FingerprintException("Error while verifying: " + e.getMessage(), e));
                }
            }
        });
    }

    @Override
    public List<DeviceUser> getUsers() throws FingerprintException {
        connection.ensureConnected();
        return new ArrayList<>();
    }

    @Override
    public synchronized void createUser(String deviceUserId, String name) throws FingerprintException {
        int uid = ZkUserCommands.toInternalUid(deviceUserId);
        final String n = name == null ? "" : name;
        connection.withWriteRetry("createUser", () -> {
            connection.disableDeviceBestEffort("before createUser");
            try {
                try {
                    users.deleteUserByUid(uid);
                } catch (FingerprintException ignored) {
                }
                users.writeUser(uid, n, deviceUserId);
            } finally {
                connection.enableDeviceBestEffort("after createUser");
            }
        });
    }

    @Override
    public synchronized void updateUserName(String deviceUserId, String name) throws FingerprintException {
        int uid = ZkUserCommands.toInternalUid(deviceUserId);
        final String n = name == null ? "" : name;
        connection.withWriteRetry("updateUserName", () -> {
            connection.disableDeviceBestEffort("before updateUserName");
            try {
                users.writeUser(uid, n, deviceUserId);
            } finally {
                connection.enableDeviceBestEffort("after updateUserName");
            }
        });
    }

    @Override
    public synchronized void deleteUser(String deviceUserId) throws FingerprintException {
        int uid = ZkUserCommands.toInternalUid(deviceUserId);
        connection.withWriteRetry("deleteUser", () -> {
            connection.disableDeviceBestEffort("before deleteUser");
            try {
                users.deleteUserByUid(uid);
            } finally {
                connection.enableDeviceBestEffort("after deleteUser");
            }
        });
    }

    @Override
    public synchronized void registerUserWithFingerprint(String deviceUserId, String name, int fingerIndex)
            throws FingerprintException {
        if (fingerIndex < 0 || fingerIndex > 9) {
            throw new FingerprintException("finger index must be 0..9");
        }
        createUser(deviceUserId, name);
        try {
            connection.withWriteRetry("registerUserWithFingerprint", () -> {
                connection.disableDeviceBestEffort("before register enroll");
                try {
                    users.sendStartEnroll(deviceUserId, fingerIndex);
                    events.waitForEnrollDeviceEvent(ZkProtocol.ENROLL_TIMEOUT_SECONDS);
                } finally {
                    connection.enableDeviceBestEffort("after registerUserWithFingerprint");
                }
            });
        } catch (FingerprintException e) {
            try {
                deleteUser(deviceUserId);
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    @Override
    public synchronized void enrollFingerOnly(String deviceUserId, int fingerIndex) throws FingerprintException {
        if (fingerIndex < 0 || fingerIndex > 9) {
            throw new FingerprintException("finger index must be 0..9");
        }
        connection.withWriteRetry("enrollFingerOnly", () -> {
            connection.disableDeviceBestEffort("before enrollFingerOnly");
            try {
                users.sendStartEnroll(deviceUserId, fingerIndex);
                events.waitForEnrollDeviceEvent(ZkProtocol.ENROLL_TIMEOUT_SECONDS);
            } finally {
                connection.enableDeviceBestEffort("after enrollFingerOnly");
            }
        });
    }

    @Override
    public void startEnroll(String deviceUserId, int fingerIndex,
                            Consumer<EnrollResult> onFinished,
                            Consumer<FingerprintException> onError) {
        executor.submit(() -> {
            try {
                enrollFingerOnly(deviceUserId, fingerIndex);
                if (onFinished != null) {
                    onFinished.accept(new EnrollResult(true, "Enroll finished for user " + deviceUserId));
                }
            } catch (FingerprintException e) {
                if (onError != null) {
                    onError.accept(e);
                }
            }
        });
    }

    @Override
    public DeviceInfo getDeviceInfo() throws FingerprintException {
        connection.ensureConnected();
        return new DeviceInfo("unknown", "unknown", "ZMM100_TFT",
                connection.getHost() + ":" + connection.getPort());
    }
}
