package com.car.rental.service.zk;

import java.nio.charset.StandardCharsets;

/**
 * ZKTeco TCP protocol constants and pure packet helpers (no I/O).
 * Layouts match the working firmware used by this project (pyzk-compatible USER_WRQ).
 */
public final class ZkProtocol {

    private ZkProtocol() {
    }

    // --- command codes ---

    public static final int CMD_CONNECT = 1000;
    public static final int CMD_EXIT = 1001;
    public static final int CMD_ENABLEDEVICE = 1002;
    public static final int CMD_DISABLEDEVICE = 1003;
    public static final int CMD_ACK_OK = 2000;
    public static final int CMD_REG_EVENT = 500;
    public static final int CMD_USER_WRQ = 8;
    public static final int CMD_DELETE_USER = 18;
    public static final int CMD_STARTVERIFY = 60;
    public static final int CMD_STARTENROLL = 61;

    // --- realtime event flags ---

    public static final int EF_ATTLOG = 1;
    public static final int EF_ENROLLFINGER = 8;

    public static final int ENROLL_TIMEOUT_SECONDS = 60;
    public static final int DEFAULT_SO_TIMEOUT_MS = 8000;

    /** Packet magic: 0x50 0x50 0x82 0x7D */
    public static final byte[] PACKET_START = new byte[]{0x50, 0x50, (byte) 0x82, 0x7D};

    public static byte[] buildPayload(int command, byte[] data, int sessionId, int replyNumber) {
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
        return payload;
    }

    public static int createChecksum(byte[] payload) {
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

    public static int getCommand(byte[] fullPacket) {
        return (fullPacket[8] & 0xFF) | ((fullPacket[9] & 0xFF) << 8);
    }

    public static int getSessionId(byte[] fullPacket) {
        return (fullPacket[12] & 0xFF) | ((fullPacket[13] & 0xFF) << 8);
    }

    public static byte[] intToBytes(int v) {
        return new byte[]{
                (byte) (v & 0xFF),
                (byte) ((v >> 8) & 0xFF),
                (byte) ((v >> 16) & 0xFF),
                (byte) ((v >> 24) & 0xFF)
        };
    }

    public static byte[] padFixed(String s, int len) {
        byte[] src = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[len];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, len));
        return out;
    }

    /**
     * 72-byte USER_WRQ body: uid(2) + privilege(1) + password(8) + name(24)
     * + card(4) + pad(1) + group(7) + pad(1) + userId(24).
     */
    public static byte[] buildUserWrqBody(int uid, String name, String userId) {
        byte[] password = padFixed("", 8);
        byte[] nameBytes = padFixed(name, 24);
        byte[] card = new byte[4];
        byte[] groupId = padFixed("", 7);
        byte[] userIdBytes = padFixed(userId, 24);

        byte[] data = new byte[72];
        int o = 0;
        data[o++] = (byte) (uid & 0xFF);
        data[o++] = (byte) ((uid >> 8) & 0xFF);
        data[o++] = 0; // privilege
        System.arraycopy(password, 0, data, o, 8);
        o += 8;
        System.arraycopy(nameBytes, 0, data, o, 24);
        o += 24;
        System.arraycopy(card, 0, data, o, 4);
        o += 4;
        data[o++] = 0;
        System.arraycopy(groupId, 0, data, o, 7);
        o += 7;
        data[o++] = 0;
        System.arraycopy(userIdBytes, 0, data, o, 24);
        return data;
    }

    public static byte[] buildDeleteUserBody(int uid) {
        return new byte[]{(byte) (uid & 0xFF), (byte) ((uid >> 8) & 0xFF)};
    }

    public static byte[] buildStartEnrollBody(String deviceUserId, int fingerIndex) {
        byte[] data = new byte[26];
        System.arraycopy(padFixed(deviceUserId, 24), 0, data, 0, 24);
        data[24] = (byte) (fingerIndex & 0xFF);
        data[25] = 1;
        return data;
    }

    public static byte[] buildRegisterAllEventsBody() {
        return new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
    }
}
