package com.car.rental.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Test: create user on ZK device + start fingerprint enroll.
 *
 * Usage: run main, enter User ID and Name, put finger when asked.
 */
public class ZkUserEnrollTest {

    private static final String HOST = "192.168.1.200";
    private static final int PORT = 4370;

    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ENABLEDEVICE = 1002;
    private static final int CMD_DISABLEDEVICE = 1003;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_USER_WRQ = 8;       // write user
    private static final int CMD_DELETE_USER = 18;
    private static final int CMD_STARTENROLL = 61;   // start enroll

    private static final byte[] PACKET_START = new byte[]{0x50, 0x50, (byte) 0x82, 0x7D};

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int sessionId;
    private int replyNumber;

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.print("Device User ID (e.g. 90001): ");
        String userId = sc.nextLine().trim();
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Internal uid number (1-65535, unique on device, e.g. 101): ");
        int uid = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Finger index (0-9, usually 0 for right thumb): ");
        int fingerIndex = Integer.parseInt(sc.nextLine().trim());

        ZkUserEnrollTest t = new ZkUserEnrollTest();
        try {
            System.out.println("Connecting...");
            t.connect();
            System.out.println("Connected. session=" + t.sessionId);

            // Optional: delete if exists (ignore errors)
            try {
                System.out.println("Trying delete old uid=" + uid + " (ignore if not exists)...");
                t.deleteUser(uid);
            } catch (Exception e) {
                System.out.println("Delete skipped: " + e.getMessage());
            }

            System.out.println("Creating user on device...");
            t.disableDevice();
            t.setUser(uid, name, userId);
            t.enableDevice();
            System.out.println("User created: uid=" + uid + ", userId=" + userId + ", name=" + name);

            System.out.println();
            System.out.println(">>> Starting ENROLL. Put the SAME finger on the sensor several times.");
            System.out.println(">>> Finger index = " + fingerIndex);
            t.startEnroll(uid, fingerIndex, userId);

            // Wait for device to process enroll (many firmwares need a few seconds + multiple scans)
            System.out.println("Waiting 25 seconds for enroll to finish on device...");
            System.out.println("(Follow device screen: place finger 3 times if asked)");
            Thread.sleep(25_000);

            System.out.println("Done. Test verify with ZkTestMain using User ID: " + userId);
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            t.disconnect();
            sc.close();
        }
    }

    // ==================== high-level ops ====================

    private void connect() throws IOException, FingerprintException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(HOST, PORT), 8000);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(8000);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        sessionId = 0;
        replyNumber = 0;

        byte[] reply = sendCommand(CMD_CONNECT, new byte[0]);
        if (reply == null || getCommand(reply) != CMD_ACK_OK) {
            throw new FingerprintException("CONNECT failed");
        }
        sessionId = getSessionId(reply);
    }

    private void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                sendCommand(CMD_EXIT, new byte[0]);
            }
        } catch (Exception ignored) {
        }
        closeQuietly();
    }

    private void disableDevice() throws IOException, FingerprintException {
        byte[] reply = sendCommand(CMD_DISABLEDEVICE, new byte[0]);
        requireAck(reply, "DISABLEDEVICE");
    }

    private void enableDevice() throws IOException, FingerprintException {
        byte[] reply = sendCommand(CMD_ENABLEDEVICE, new byte[0]);
        requireAck(reply, "ENABLEDEVICE");
    }

    /**
     * Same packing as pyzk for TFT (user_packet_size != 28):
     * pack('HB8s24s4sx7sx24s', uid, privilege, password, name, card, group_id, user_id)
     * Total payload = 72 bytes
     */
    private void setUser(int uid, String name, String userId) throws IOException, FingerprintException {
        byte[] password = padFixed("", 8);
        byte[] nameBytes = padFixed(name, 24);
        byte[] card = new byte[]{0, 0, 0, 0};          // card number 0
        byte[] groupId = padFixed("", 7);             // empty group string (7 bytes)
        byte[] userIdBytes = padFixed(userId, 24);

        byte[] data = new byte[72];
        int o = 0;

        // H uid
        data[o++] = (byte) (uid & 0xFF);
        data[o++] = (byte) ((uid >> 8) & 0xFF);
        // B privilege
        data[o++] = 0;
        // 8s password
        System.arraycopy(password, 0, data, o, 8); o += 8;
        // 24s name
        System.arraycopy(nameBytes, 0, data, o, 24); o += 24;
        // 4s card
        System.arraycopy(card, 0, data, o, 4); o += 4;
        // x (1 byte padding)
        data[o++] = 0;
        // 7s group_id
        System.arraycopy(groupId, 0, data, o, 7); o += 7;
        // x (1 byte padding)
        data[o++] = 0;
        // 24s user_id
        System.arraycopy(userIdBytes, 0, data, o, 24);

        System.out.println("SET_USER payload (" + data.length + " bytes): " + toHex(data));
        byte[] reply = sendCommand(CMD_USER_WRQ, data);
        requireAck(reply, "USER_WRQ");
    }

    private void deleteUser(int uid) throws IOException, FingerprintException {
        byte[] data = new byte[2];
        data[0] = (byte) (uid & 0xFF);
        data[1] = (byte) ((uid >> 8) & 0xFF);
        byte[] reply = sendCommand(CMD_DELETE_USER, data);
        requireAck(reply, "DELETE_USER");
    }

    /**
     * pyzk-style: pack('<24sbb', user_id, temp_id, 1)
     */
    private void startEnroll(int uid, int fingerIndex, String userId) throws IOException, FingerprintException {
        byte[] data = new byte[26]; // 24 + 1 + 1
        byte[] idBytes = padFixed(userId, 24);
        System.arraycopy(idBytes, 0, data, 0, 24);
        data[24] = (byte) (fingerIndex & 0xFF);
        data[25] = 1; // flag used by pyzk

        System.out.println("STARTENROLL payload: " + toHex(data));
        byte[] reply = sendCommand(CMD_STARTENROLL, data);
        int cmd = reply == null ? -1 : getCommand(reply);
        System.out.println("STARTENROLL reply cmd=" + cmd + " raw=" + (reply == null ? "null" : toHex(reply)));
        if (reply == null || cmd != CMD_ACK_OK) {
            throw new FingerprintException("STARTENROLL failed");
        }
        System.out.println("Enroll mode accepted. Place finger on device when it asks.");
    }

    // ==================== protocol (same as working ZkFingerprintService) ====================

    private byte[] sendCommand(int command, byte[] data) throws IOException {
        int replyId = replyNumber & 0xFFFF;
        replyNumber = (replyNumber + 1) & 0xFFFF;
        byte[] packet = buildPacket(command, sessionId, replyId, data);
        System.out.println("SEND: " + toHex(packet));
        out.write(packet);
        out.flush();
        byte[] reply = readPacket();
        System.out.println("RECV: " + (reply == null ? "null" : toHex(reply)));
        return reply;
    }

    private byte[] buildPacket(int command, int session, int replyId, byte[] data) {
        int dataLen = data == null ? 0 : data.length;
        int payloadSize = 8 + dataLen;
        byte[] payload = new byte[payloadSize];
        payload[0] = (byte) (command & 0xFF);
        payload[1] = (byte) ((command >> 8) & 0xFF);
        payload[2] = 0;
        payload[3] = 0;
        payload[4] = (byte) (session & 0xFF);
        payload[5] = (byte) ((session >> 8) & 0xFF);
        payload[6] = (byte) (replyId & 0xFF);
        payload[7] = (byte) ((replyId >> 8) & 0xFF);
        if (dataLen > 0) {
            System.arraycopy(data, 0, payload, 8, dataLen);
        }
        int cs = createChecksum(payload);
        payload[2] = (byte) (cs & 0xFF);
        payload[3] = (byte) ((cs >> 8) & 0xFF);

        byte[] packet = new byte[8 + payloadSize];
        System.arraycopy(PACKET_START, 0, packet, 0, 4);
        packet[4] = (byte) (payloadSize & 0xFF);
        packet[5] = (byte) ((payloadSize >> 8) & 0xFF);
        packet[6] = (byte) ((payloadSize >> 16) & 0xFF);
        packet[7] = (byte) ((payloadSize >> 24) & 0xFF);
        System.arraycopy(payload, 0, packet, 8, payloadSize);
        return packet;
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
        if ((header[0] & 0xFF) != 0x50 || (header[1] & 0xFF) != 0x50) return null;
        int payloadSize = (header[4] & 0xFF)
                | ((header[5] & 0xFF) << 8)
                | ((header[6] & 0xFF) << 16)
                | ((header[7] & 0xFF) << 24);
        if (payloadSize < 8 || payloadSize > 1024 * 1024) return null;
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

    private int getCommand(byte[] full) {
        return (full[8] & 0xFF) | ((full[9] & 0xFF) << 8);
    }

    private int getSessionId(byte[] full) {
        return (full[12] & 0xFF) | ((full[13] & 0xFF) << 8);
    }

    private void requireAck(byte[] reply, String op) throws FingerprintException {
        if (reply == null || getCommand(reply) != CMD_ACK_OK) {
            throw new FingerprintException(op + " not ACK_OK");
        }
    }

    private static byte[] padFixed(String s, int len) {
        byte[] src = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[len];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, len));
        return out;
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", data[i]));
        }
        return sb.toString();
    }

    private void closeQuietly() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}