package com.car.rental.service.zk;

import com.car.rental.service.FingerprintException;
import com.car.rental.service.VerificationResult;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Blocking enroll wait and one-shot verification listen over an open {@link ZkConnection}.
 */
public class ZkEventListener {

    private static final Logger logger = Logger.getLogger(ZkEventListener.class.getName());

    private final ZkConnection connection;
    private final AtomicBoolean enrolling;
    private final AtomicBoolean listening;

    public ZkEventListener(ZkConnection connection, AtomicBoolean enrolling, AtomicBoolean listening) {
        this.connection = connection;
        this.enrolling = enrolling;
        this.listening = listening;
    }

    public void waitForEnrollDeviceEvent(int timeoutSeconds) throws FingerprintException, IOException {
        enrolling.set(true);
        byte[] regReply = connection.sendCommand(
                ZkProtocol.CMD_REG_EVENT, ZkProtocol.buildRegisterAllEventsBody());
        if (regReply == null || ZkProtocol.getCommand(regReply) != ZkProtocol.CMD_ACK_OK) {
            throw new FingerprintException("Failed to register enroll events");
        }
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int previousTimeout = connection.getSoTimeout();
        connection.setSoTimeout(1000);
        try {
            while (enrolling.get() && System.currentTimeMillis() < deadline) {
                try {
                    byte[] packet = connection.readPacket();
                    if (packet == null) {
                        continue;
                    }
                    if (ZkProtocol.getCommand(packet) != ZkProtocol.CMD_REG_EVENT) {
                        continue;
                    }
                    int eventCode = ZkProtocol.getSessionId(packet);
                    if (eventCode == ZkProtocol.EF_ENROLLFINGER
                            || (eventCode & ZkProtocol.EF_ENROLLFINGER) != 0) {
                        int result = packet.length > 16 ? (packet[16] & 0xFF) : 0;
                        if (result == 0) {
                            logger.info("Enroll success event");
                            enrolling.set(false);
                            return;
                        }
                        throw new FingerprintException("Enroll failed on device (result=" + result + ")");
                    }
                } catch (SocketTimeoutException ste) {
                    // poll
                }
            }
            if (!enrolling.get()) {
                throw new FingerprintException("ثبت اثر انگشت توسط کاربر لغو شد");
            }
            throw new FingerprintException("Enroll timed out after " + timeoutSeconds + "s");
        } finally {
            try {
                connection.setSoTimeout(previousTimeout);
            } catch (Exception ignored) {
            }
            enrolling.set(false);
        }
    }

    /**
     * Blocking one-shot verify loop. Caller is responsible for connect and STARTVERIFY setup.
     */
    public void listenUntilVerifiedOrTimeout(
            int timeoutSeconds,
            Consumer<VerificationResult> onVerified,
            Runnable onTimeout) throws IOException, FingerprintException {

        byte[] regReply = connection.sendCommand(
                ZkProtocol.CMD_REG_EVENT, ZkProtocol.buildRegisterAllEventsBody());
        if (regReply == null || ZkProtocol.getCommand(regReply) != ZkProtocol.CMD_ACK_OK) {
            throw new FingerprintException("Failed to register realtime events");
        }

        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int previousTimeout = connection.getSoTimeout();
        connection.setSoTimeout(1000);
        try {
            while (listening.get() && System.currentTimeMillis() < deadline) {
                try {
                    byte[] packet = connection.readPacket();
                    if (packet == null) {
                        continue;
                    }
                    if (ZkProtocol.getCommand(packet) != ZkProtocol.CMD_REG_EVENT) {
                        continue;
                    }
                    int eventCode = ZkProtocol.getSessionId(packet);
                    if (eventCode == ZkProtocol.EF_ATTLOG || (eventCode & ZkProtocol.EF_ATTLOG) != 0) {
                        VerificationResult result = ZkAttLogParser.parse(packet);
                        if (result != null) {
                            listening.set(false);
                            if (onVerified != null) {
                                onVerified.accept(result);
                            }
                            return;
                        }
                    }
                } catch (SocketTimeoutException ste) {
                    // poll
                }
            }
            if (listening.get()) {
                listening.set(false);
                if (onTimeout != null) {
                    onTimeout.run();
                }
            }
        } finally {
            try {
                connection.setSoTimeout(previousTimeout);
            } catch (Exception ignored) {
            }
        }
    }
}
