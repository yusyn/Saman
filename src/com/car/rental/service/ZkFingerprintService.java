package com.car.rental.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ZKTeco TCP client (port 4370).
 * Full create/update/delete/enroll restored (no stub exceptions).
 * DISABLEDEVICE is best-effort; user write ops retry once on transient timeout.
 */
public class ZkFingerprintService implements FingerprintService {

    private static final Logger logger = Logger.getLogger(ZkFingerprintService.class.getName());

    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ENABLEDEVICE = 1002;
    private static final int CMD_DISABLEDEVICE = 1003;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_REG_EVENT = 500;
    private static final int CMD_USER_WRQ = 8;
    private static final int CMD_DELETE_USER = 18;
    private static final int CMD_STARTVERIFY = 60;
    private static final int CMD_STARTENROLL = 61;

    private static final int EF_ATTLOG = 1;
    private static final int EF_ENROLLFINGER = 8;

    private static final int ENROLL_TIMEOUT_SECONDS = 60;
    private static final byte[] PACKET_START = new byte[]{0x50, 0x50, (byte) 0x82, 0x7D};

    private final String host;
    private final int port;
    private final int connectTimeoutMs;

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int sessionId;
    private int replyNumber;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private final AtomicBoolean enrolling = new AtomicBoolean(false);

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
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public ZkFingerprintService() {
        this("192.168.20.200", 4370, 8000);
    }

    @Override
    public synchronized void connect() throws FingerprintException {
        if (connected.get()) {
            enableDeviceBestEffort("existing connection");
            return;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(8000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            sessionId = 0;
            replyNumber = 0;

            byte[] reply = sendCommand(CMD_CONNECT, new byte[0]);
            if (reply == null) {
                closeQuietly();
                throw new FingerprintException("No reply from device on CONNECT");
            }
            if (getCommand(reply) != CMD_ACK_OK) {
                closeQuietly();
                throw new FingerprintException("Device rejected CONNECT");
            }
            sessionId = getSessionId(reply);
            connected.set(true);
            enableDeviceBestEffort("after connect");
            logger.info("Connected to ZK " + host + ":" + port + " session=" + sessionId);
        } catch (IOException e) {
            closeQuietly();
            throw new FingerprintException("Cannot connect to device " + host + ":" + port, e);
        }
    }

    @Override
    public synchronized void disconnect() {
        cancelListen();
        cancelEnroll();
        if (!connected.get()) {
            return;
        }
        try {
            enableDeviceBestEffort("before disconnect");
            sendCommand(CMD_EXIT, new byte[0]);
        } catch (Exception ignored) {
        }
        closeQuietly();
        connected.set(false);
        logger.info("Disconnected from ZK device");
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
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

    private void ensureConnected() throws FingerprintException {
        if (!isConnected()) {
            connect();
        }
    }

    private void enableDeviceBestEffort(String context) {
        try {
            if (!isConnected()) return;
            sendCommand(CMD_ENABLEDEVICE, new byte[0]);
        } catch (Exception e) {
            logger.info("ENABLEDEVICE skipped/ignored after " + context);
        }
    }

    /** Best-effort DISABLEDEVICE — many firmwares are slow/silent; do not fail the whole op. */
    private void disableDeviceBestEffort(String context) {
        try {
            if (!isConnected()) return;
            sendCommand(CMD_DISABLEDEVICE, new byte[0]);
        } catch (Exception e) {
            logger.info("DISABLEDEVICE skipped/ignored after " + context + ": " + e.getMessage());
        }
    }

    private static boolean isTransientIo(Throwable e) {
        while (e != null) {
            if (e instanceof java.net.SocketTimeoutException) {
                return true;
            }
            String msg = e.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("timed out") || m.contains("read timed out") || m.contains("connection reset")) {
                    return true;
                }
            }
            e = e.getCause();
        }
        return false;
    }

    /** Run a device write once; on transient timeout/reset, reconnect and retry once. */
    private void withWriteRetry(String opName, WriteAction action) throws FingerprintException {
        FingerprintException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ensureConnected();
                action.run();
                return;
            } catch (FingerprintException e) {
                last = e;
                if (attempt == 1 && isTransientIo(e)) {
                    logger.info(opName + " transient failure, reconnecting for retry: " + e.getMessage());
                    try {
                        disconnect();
                    } catch (Exception ignored) {
                    }
                    continue;
                }
                throw e;
            } catch (IOException e) {
                last = new FingerprintException(opName + " failed: " + e.getMessage(), e);
                if (attempt == 1 && isTransientIo(e)) {
                    logger.info(opName + " transient IO, reconnecting for retry: " + e.getMessage());
                    try {
                        disconnect();
                    } catch (Exception ignored) {
                    }
                    continue;
                }
                throw last;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    @FunctionalInterface
    private interface WriteAction {
        void run() throws IOException, FingerprintException;
    }

    private synchronized byte[] sendCommand(int command, byte[] data) throws IOException {
        if (out == null) throw new IOException("Not connected");
        byte[] payload = buildPayload(command, data);
        out.write(PACKET_START);
        out.write(intToBytes(payload.length));
        out.write(payload);
        out.flush();
        return readPacket();
    }

    private byte[] buildPayload(int command, byte[] data) {
        byte[] payload = new byte[8 + data.length];
        payload[0] = (byte) (command & 0xFF);
        payload[1] = (byte) ((command >> 8) & 0xFF);
        payload[2] = 0;
        payload[3] = 0;
        payload[4] = (byte) (sessionId & 0xFF);
        payload[5] = (byte) ((sessionId >> 8) & 0xFF);
        payload[6] = (byte) (replyNumber & 0xFF);
        payload[7] = (byte) ((replyNumber >> 8) & 0xFF);
        System.arraycopy(data, 0, payload, 8, data.length);
        int chk = createChecksum(payload);
        payload[2] = (byte) (chk & 0xFF);
        payload[3] = (byte) ((chk >> 8) & 0xFF);
        replyNumber = (replyNumber + 1) & 0xFFFF;
        return payload;
    }

    private static int createChecksum(byte[] payload) {
        int chksum = 0;
        for (int i = 0; i < payload.length; i += 2) {
            if (i == payload.length - 1) {
                chksum += payload[i] & 0xFF;
            } else {
                chksum += (payload[i] & 0xFF) + ((payload[i + 1] & 0xFF) << 8);
            }
            if (chksum > 65535) {
                chksum -= 65535;
            }
        }
        chksum = ~chksum;
        while (chksum < 0) {
            chksum += 65536;
        }
        return chksum & 0xFFFF;
    }

    private byte[] readPacket() throws IOException {
        byte[] header = readFully(8);
        if (header == null) return null;
        if ((header[0] & 0xFF) != 0x50 || (header[1] & 0xFF) != 0x50) {
            return null;
        }
        int payloadSize = (header[4] & 0xFF)
                | ((header[5] & 0xFF) << 8)
                | ((header[6] & 0xFF) << 16)
                | ((header[7] & 0xFF) << 24);
        if (payloadSize < 8 || payloadSize > 1024 * 1024) {
            return null;
        }
        byte[] payload = readFully(payloadSize);
        if (payload == null) return null;
        byte[] full = new byte[8 + payloadSize];
        System.arraycopy(header, 0, full, 0, 8);
        System.arraycopy(payload, 0, full, 8, payloadSize);
        return full;
    }

    private byte[] readFully(int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n < 0) return null;
            off += n;
        }
        return buf;
    }

    private int getCommand(byte[] fullPacket) {
        return (fullPacket[8] & 0xFF) | ((fullPacket[9] & 0xFF) << 8);
    }

    private int getSessionId(byte[] fullPacket) {
        return (fullPacket[12] & 0xFF) | ((fullPacket[13] & 0xFF) << 8);
    }

    private static byte[] intToBytes(int v) {
        return new byte[]{
            (byte) (v & 0xFF),
            (byte) ((v >> 8) & 0xFF),
            (byte) ((v >> 16) & 0xFF),
            (byte) ((v >> 24) & 0xFF)
        };
    }

    private void closeQuietly() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }

    private static int toInternalUid(String deviceUserId) throws FingerprintException {
        try {
            int uid = Integer.parseInt(deviceUserId.trim());
            if (uid < 1 || uid > 65535) {
                throw new FingerprintException("deviceUserId out of range: " + deviceUserId);
            }
            return uid;
        } catch (NumberFormatException e) {
            throw new FingerprintException("deviceUserId must be numeric: " + deviceUserId, e);
        }
    }

    private static byte[] padFixed(String s, int len) {
        byte[] src = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[len];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, len));
        return out;
    }

    /** 72-byte USER_WRQ layout matching working device firmware (pyzk-compatible). */
    private void writeUser(int uid, String name, String userId) throws IOException, FingerprintException {
        byte[] password = padFixed("", 8);
        byte[] nameBytes = padFixed(name, 24);
        byte[] card = new byte[4];
        byte[] groupId = padFixed("", 7);
        byte[] userIdBytes = padFixed(userId, 24);

        byte[] data = new byte[72];
        int o = 0;
        data[o++] = (byte) (uid & 0xFF);
        data[o++] = (byte) ((uid >> 8) & 0xFF);
        data[o++] = 0;
        System.arraycopy(password, 0, data, o, 8); o += 8;
        System.arraycopy(nameBytes, 0, data, o, 24); o += 24;
        System.arraycopy(card, 0, data, o, 4); o += 4;
        data[o++] = 0;
        System.arraycopy(groupId, 0, data, o, 7); o += 7;
        data[o++] = 0;
        System.arraycopy(userIdBytes, 0, data, o, 24);

        byte[] reply = sendCommand(CMD_USER_WRQ, data);
        if (reply == null || getCommand(reply) != CMD_ACK_OK) {
            throw new FingerprintException("USER_WRQ failed for uid=" + uid);
        }
        logger.info("USER_WRQ OK uid=" + uid + " name=" + name);
    }

    private void deleteUserByUid(int uid) throws IOException, FingerprintException {
        byte[] data = new byte[]{(byte) (uid & 0xFF), (byte) ((uid >> 8) & 0xFF)};
        byte[] reply = sendCommand(CMD_DELETE_USER, data);
        if (reply == null || getCommand(reply) != CMD_ACK_OK) {
            throw new FingerprintException("DELETE_USER failed for uid=" + uid);
        }
        logger.info("DELETE_USER OK uid=" + uid);
    }

    private void sendStartEnroll(String deviceUserId, int fingerIndex) throws IOException, FingerprintException {
        byte[] data = new byte[26];
        System.arraycopy(padFixed(deviceUserId, 24), 0, data, 0, 24);
        data[24] = (byte) (fingerIndex & 0xFF);
        data[25] = 1;
        byte[] reply = sendCommand(CMD_STARTENROLL, data);
        if (reply == null || getCommand(reply) != CMD_ACK_OK) {
            throw new FingerprintException("STARTENROLL rejected for user " + deviceUserId);
        }
        logger.info("STARTENROLL OK user=" + deviceUserId + " finger=" + fingerIndex);
    }

    private void waitForEnrollDeviceEvent(int timeoutSeconds) throws FingerprintException, IOException {
        enrolling.set(true);
        byte[] regData = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
        byte[] regReply = sendCommand(CMD_REG_EVENT, regData);
        if (regReply == null || getCommand(regReply) != CMD_ACK_OK) {
            throw new FingerprintException("Failed to register enroll events");
        }
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int previousTimeout = socket.getSoTimeout();
        socket.setSoTimeout(1000);
        try {
            while (enrolling.get() && System.currentTimeMillis() < deadline) {
                try {
                    byte[] packet = readPacket();
                    if (packet == null) continue;
                    if (getCommand(packet) != CMD_REG_EVENT) continue;
                    int eventCode = getSessionId(packet);
                    if (eventCode == EF_ENROLLFINGER || (eventCode & EF_ENROLLFINGER) != 0) {
                        int result = packet.length > 16 ? (packet[16] & 0xFF) : 0;
                        if (result == 0) {
                            logger.info("Enroll success event");
                            enrolling.set(false);
                            return;
                        }
                        throw new FingerprintException("Enroll failed on device (result=" + result + ")");
                    }
                } catch (java.net.SocketTimeoutException ste) {
                    // poll
                }
            }
            if (!enrolling.get()) {
                throw new FingerprintException("ثبت اثر انگشت توسط کاربر لغو شد");
            }
            throw new FingerprintException("Enroll timed out after " + timeoutSeconds + "s");
        } finally {
            try { socket.setSoTimeout(previousTimeout); } catch (Exception ignored) {}
            enrolling.set(false);
        }
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
                if (!isConnected()) connect();
                synchronized (ZkFingerprintService.this) {
                    enableDeviceBestEffort("before verify");
                    try { sendCommand(CMD_STARTVERIFY, new byte[0]); } catch (Exception ignored) {}
                }
                byte[] regData = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
                byte[] regReply = sendCommand(CMD_REG_EVENT, regData);
                if (regReply == null || getCommand(regReply) != CMD_ACK_OK) {
                    throw new FingerprintException("Failed to register realtime events");
                }
                long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
                int previousTimeout = socket.getSoTimeout();
                socket.setSoTimeout(1000);
                try {
                    while (listening.get() && System.currentTimeMillis() < deadline) {
                        try {
                            byte[] packet = readPacket();
                            if (packet == null) continue;
                            if (getCommand(packet) != CMD_REG_EVENT) continue;
                            int eventCode = getSessionId(packet);
                            if (eventCode == EF_ATTLOG || (eventCode & EF_ATTLOG) != 0) {
                                VerificationResult result = parseSimpleAttLog(packet);
                                if (result != null) {
                                    listening.set(false);
                                    if (onVerified != null) onVerified.accept(result);
                                    return;
                                }
                            }
                        } catch (java.net.SocketTimeoutException ste) {
                            // poll
                        }
                    }
                    if (listening.get()) {
                        listening.set(false);
                        if (onTimeout != null) onTimeout.run();
                    }
                } finally {
                    try { socket.setSoTimeout(previousTimeout); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                listening.set(false);
                if (onError != null) {
                    onError.accept(new FingerprintException("Error while verifying: " + e.getMessage(), e));
                }
            }
        });
    }

    private VerificationResult parseSimpleAttLog(byte[] fullPacket) {
        try {
            int dataOffset = 16;
            if (fullPacket.length <= dataOffset) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = dataOffset; i < Math.min(fullPacket.length, dataOffset + 24); i++) {
                int b = fullPacket[i] & 0xFF;
                if (b == 0) break;
                if (b >= '0' && b <= '9') sb.append((char) b);
                else if (sb.length() > 0) break;
            }
            if (sb.length() == 0 && fullPacket.length >= dataOffset + 2) {
                int uid16 = (fullPacket[dataOffset] & 0xFF) | ((fullPacket[dataOffset + 1] & 0xFF) << 8);
                if (uid16 >= 1 && uid16 <= 30000) sb.append(uid16);
            }
            if (sb.length() == 0) return null;
            return new VerificationResult(sb.toString(), LocalDateTime.now(), 1);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<DeviceUser> getUsers() throws FingerprintException {
        ensureConnected();
        return new ArrayList<>();
    }

    @Override
    public synchronized void createUser(String deviceUserId, String name) throws FingerprintException {
        int uid = toInternalUid(deviceUserId);
        final String n = name == null ? "" : name;
        withWriteRetry("createUser", () -> {
            disableDeviceBestEffort("before createUser");
            try {
                try {
                    deleteUserByUid(uid);
                } catch (FingerprintException ignored) {
                }
                writeUser(uid, n, deviceUserId);
            } finally {
                enableDeviceBestEffort("after createUser");
            }
        });
    }

    @Override
    public synchronized void updateUserName(String deviceUserId, String name) throws FingerprintException {
        int uid = toInternalUid(deviceUserId);
        final String n = name == null ? "" : name;
        withWriteRetry("updateUserName", () -> {
            disableDeviceBestEffort("before updateUserName");
            try {
                writeUser(uid, n, deviceUserId);
            } finally {
                enableDeviceBestEffort("after updateUserName");
            }
        });
    }

    @Override
    public synchronized void deleteUser(String deviceUserId) throws FingerprintException {
        int uid = toInternalUid(deviceUserId);
        withWriteRetry("deleteUser", () -> {
            disableDeviceBestEffort("before deleteUser");
            try {
                deleteUserByUid(uid);
            } finally {
                enableDeviceBestEffort("after deleteUser");
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
            withWriteRetry("registerUserWithFingerprint", () -> {
                disableDeviceBestEffort("before register enroll");
                try {
                    sendStartEnroll(deviceUserId, fingerIndex);
                    waitForEnrollDeviceEvent(ENROLL_TIMEOUT_SECONDS);
                } finally {
                    enableDeviceBestEffort("after registerUserWithFingerprint");
                }
            });
        } catch (FingerprintException e) {
            try { deleteUser(deviceUserId); } catch (Exception ignored) {}
            throw e;
        }
    }

    @Override
    public synchronized void enrollFingerOnly(String deviceUserId, int fingerIndex) throws FingerprintException {
        if (fingerIndex < 0 || fingerIndex > 9) {
            throw new FingerprintException("finger index must be 0..9");
        }
        withWriteRetry("enrollFingerOnly", () -> {
            disableDeviceBestEffort("before enrollFingerOnly");
            try {
                sendStartEnroll(deviceUserId, fingerIndex);
                waitForEnrollDeviceEvent(ENROLL_TIMEOUT_SECONDS);
            } finally {
                enableDeviceBestEffort("after enrollFingerOnly");
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
                if (onError != null) onError.accept(e);
            }
        });
    }

    @Override
    public DeviceInfo getDeviceInfo() throws FingerprintException {
        ensureConnected();
        return new DeviceInfo("unknown", "unknown", "ZMM100_TFT", host + ":" + port);
    }
}
