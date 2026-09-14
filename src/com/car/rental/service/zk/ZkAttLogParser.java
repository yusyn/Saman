package com.car.rental.service.zk;

import com.car.rental.service.VerificationResult;

import java.time.LocalDateTime;

/**
 * Parses a simple attendance/verification event payload from the device.
 */
public final class ZkAttLogParser {

    private ZkAttLogParser() {
    }

    public static VerificationResult parse(byte[] fullPacket) {
        try {
            int dataOffset = 16;
            if (fullPacket.length <= dataOffset) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = dataOffset; i < Math.min(fullPacket.length, dataOffset + 24); i++) {
                int b = fullPacket[i] & 0xFF;
                if (b == 0) {
                    break;
                }
                if (b >= '0' && b <= '9') {
                    sb.append((char) b);
                } else if (sb.length() > 0) {
                    break;
                }
            }
            if (sb.length() == 0 && fullPacket.length >= dataOffset + 2) {
                int uid16 = (fullPacket[dataOffset] & 0xFF) | ((fullPacket[dataOffset + 1] & 0xFF) << 8);
                if (uid16 >= 1 && uid16 <= 30000) {
                    sb.append(uid16);
                }
            }
            if (sb.length() == 0) {
                return null;
            }
            return new VerificationResult(sb.toString(), LocalDateTime.now(), 1);
        } catch (Exception e) {
            return null;
        }
    }
}
