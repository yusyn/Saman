package com.car.rental.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ZKTeco TCP client (port 4370).
 * Verification: ENABLE + STARTVERIFY + REG_EVENT, wait for ATTLOG.
 * Enroll: STARTENROLL + EF_ENROLLFINGER realtime events.
 */
public class ZkFingerprintService implements FingerprintService {

    private static final Logger logger = Logger.getLogger(ZkFingerprintService.class.getName());

    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ENABLEDEVICE = 1002;
    private static final int CMD_DISABLEDEVICE = 1003;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_ACK_ERROR = 2001;
    private static final int CMD_PREPARE_DATA = 1500;
    private static final int CMD_DATA = 1501;
    private static final int CMD_REG_EVENT = 500;
    private static final int CMD_USER_WRQ = 8;
    private static final int CMD_USERTEMP_RRQ = 9;
    private static final int CMD_DELETE_USER = 18;
    private static final int CMD_STARTVERIFY = 60;
    private static final int CMD_STARTENROLL = 61;
    private static final int CMD_WRITE_LCD = 66;
    private static final int CMD_CLEAR_LCD = 67;
    private static final int CMD_TESTVOICE = 1017;

    private static final int EF_ATTLOG = 1;
    private static final int EF_FINGER = 2;
    private static final int EF_ENROLLUSER = 4;
    private static final int EF_ENROLLFINGER = 8;
    private static final int EF_FPFTR = 256;

    private static final int ENROLL_TIMEOUT_SECONDS = 60;

    private static final long MAX_TIME_DRIFT_HOURS = 48;

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
        this("192.168.30.210", 4370, 8000);
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
                            int cmd = getCommand(packet);
                            if (cmd != CMD_REG_EVENT) continue;
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
        ensureConnected();
        throw new FingerprintException("createUser not fully restored — pull previous full file from commit 31788419");
    }

    @Override
    public synchronized void deleteUser(String deviceUserId) throws FingerprintException {
        ensureConnected();
        throw new FingerprintException("deleteUser not fully restored — pull previous full file from commit 31788419");
    }

    @Override
    public synchronized EnrollResult enrollFingerOnly(String deviceUserId, int fingerIndex) throws FingerprintException {
        ensureConnected();
        throw new FingerprintException("enrollFingerOnly not fully restored — pull previous full file from commit 31788419");
    }

    @Override
    public synchronized EnrollResult registerUserWithFingerprint(String deviceUserId, String name, int fingerIndex)
            throws FingerprintException {
        ensureConnected();
        throw new FingerprintException("registerUserWithFingerprint not fully restored — pull previous full file from commit 31788419");
    }

    @Override
    public void cancelEnroll() {
        // no-op in reduced restore
    }

    @Override
    public DeviceInfo getDeviceInfo() throws FingerprintException {
        ensureConnected();
        return new DeviceInfo("unknown", "unknown", "ZMM100_TFT", host + ":" + port);
    }
}
