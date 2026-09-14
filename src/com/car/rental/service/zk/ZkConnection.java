package com.car.rental.service.zk;

import com.car.rental.service.FingerprintException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

/**
 * TCP session to a ZK device: connect/disconnect, send/receive packets, soft enable/disable, write retry.
 */
public class ZkConnection {

    private static final Logger logger = Logger.getLogger(ZkConnection.class.getName());

    private final String host;
    private final int port;
    private final int connectTimeoutMs;

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int sessionId;
    private int replyNumber;
    private boolean connected;

    public ZkConnection(String host, int port, int connectTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public synchronized void connect() throws FingerprintException {
        if (isConnected()) {
            enableDeviceBestEffort("existing connection");
            return;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(ZkProtocol.DEFAULT_SO_TIMEOUT_MS);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            sessionId = 0;
            replyNumber = 0;

            byte[] reply = sendCommand(ZkProtocol.CMD_CONNECT, new byte[0]);
            if (reply == null) {
                closeQuietly();
                throw new FingerprintException("No reply from device on CONNECT");
            }
            if (ZkProtocol.getCommand(reply) != ZkProtocol.CMD_ACK_OK) {
                closeQuietly();
                throw new FingerprintException("Device rejected CONNECT");
            }
            sessionId = ZkProtocol.getSessionId(reply);
            connected = true;
            enableDeviceBestEffort("after connect");
            logger.info("Connected to ZK " + host + ":" + port + " session=" + sessionId);
        } catch (IOException e) {
            closeQuietly();
            throw new FingerprintException("Cannot connect to device " + host + ":" + port, e);
        }
    }

    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        try {
            enableDeviceBestEffort("before disconnect");
            sendCommand(ZkProtocol.CMD_EXIT, new byte[0]);
        } catch (Exception ignored) {
        }
        closeQuietly();
        connected = false;
        logger.info("Disconnected from ZK device");
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void ensureConnected() throws FingerprintException {
        if (!isConnected()) {
            connect();
        }
    }

    public void enableDeviceBestEffort(String context) {
        try {
            if (!isConnected()) {
                return;
            }
            sendCommand(ZkProtocol.CMD_ENABLEDEVICE, new byte[0]);
        } catch (Exception e) {
            logger.info("ENABLEDEVICE skipped/ignored after " + context);
        }
    }

    public void disableDeviceBestEffort(String context) {
        try {
            if (!isConnected()) {
                return;
            }
            sendCommand(ZkProtocol.CMD_DISABLEDEVICE, new byte[0]);
        } catch (Exception e) {
            logger.info("DISABLEDEVICE skipped/ignored after " + context + ": " + e.getMessage());
        }
    }

    /**
     * Run a device write once; on transient timeout/reset, reconnect and retry once.
     */
    public void withWriteRetry(String opName, WriteAction action) throws FingerprintException {
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
    public interface WriteAction {
        void run() throws IOException, FingerprintException;
    }

    public synchronized byte[] sendCommand(int command, byte[] data) throws IOException {
        if (out == null) {
            throw new IOException("Not connected");
        }
        byte[] payload = ZkProtocol.buildPayload(command, data, sessionId, replyNumber);
        replyNumber = (replyNumber + 1) & 0xFFFF;
        out.write(ZkProtocol.PACKET_START);
        out.write(ZkProtocol.intToBytes(payload.length));
        out.write(payload);
        out.flush();
        return readPacket();
    }

    public byte[] readPacket() throws IOException {
        byte[] header = readFully(8);
        if (header == null) {
            return null;
        }
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
        if (payload == null) {
            return null;
        }
        byte[] full = new byte[8 + payloadSize];
        System.arraycopy(header, 0, full, 0, 8);
        System.arraycopy(payload, 0, full, 8, payloadSize);
        return full;
    }

    public int getSoTimeout() throws IOException {
        if (socket == null) {
            throw new IOException("Not connected");
        }
        return socket.getSoTimeout();
    }

    public void setSoTimeout(int ms) throws IOException {
        if (socket == null) {
            throw new IOException("Not connected");
        }
        socket.setSoTimeout(ms);
    }

    private byte[] readFully(int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n < 0) {
                return null;
            }
            off += n;
        }
        return buf;
    }

    private void closeQuietly() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        in = null;
        out = null;
        socket = null;
        connected = false;
    }

    public static boolean isTransientIo(Throwable e) {
        while (e != null) {
            if (e instanceof SocketTimeoutException) {
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
}
