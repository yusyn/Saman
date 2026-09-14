package com.car.rental.service.zk;

import com.car.rental.service.FingerprintException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * User-related ZK commands: USER_WRQ, DELETE_USER, STARTENROLL.
 */
public class ZkUserCommands {

    private static final Logger logger = Logger.getLogger(ZkUserCommands.class.getName());

    private final ZkConnection connection;

    public ZkUserCommands(ZkConnection connection) {
        this.connection = connection;
    }

    public static int toInternalUid(String deviceUserId) throws FingerprintException {
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

    public void writeUser(int uid, String name, String userId) throws IOException, FingerprintException {
        byte[] data = ZkProtocol.buildUserWrqBody(uid, name, userId);
        byte[] reply = connection.sendCommand(ZkProtocol.CMD_USER_WRQ, data);
        if (reply == null || ZkProtocol.getCommand(reply) != ZkProtocol.CMD_ACK_OK) {
            throw new FingerprintException("USER_WRQ failed for uid=" + uid);
        }
        logger.info("USER_WRQ OK uid=" + uid + " name=" + name);
    }

    public void deleteUserByUid(int uid) throws IOException, FingerprintException {
        byte[] data = ZkProtocol.buildDeleteUserBody(uid);
        byte[] reply = connection.sendCommand(ZkProtocol.CMD_DELETE_USER, data);
        if (reply == null || ZkProtocol.getCommand(reply) != ZkProtocol.CMD_ACK_OK) {
            throw new FingerprintException("DELETE_USER failed for uid=" + uid);
        }
        logger.info("DELETE_USER OK uid=" + uid);
    }

    public void sendStartEnroll(String deviceUserId, int fingerIndex) throws IOException, FingerprintException {
        byte[] data = ZkProtocol.buildStartEnrollBody(deviceUserId, fingerIndex);
        byte[] reply = connection.sendCommand(ZkProtocol.CMD_STARTENROLL, data);
        if (reply == null || ZkProtocol.getCommand(reply) != ZkProtocol.CMD_ACK_OK) {
            throw new FingerprintException("STARTENROLL rejected for user " + deviceUserId);
        }
        logger.info("STARTENROLL OK user=" + deviceUserId + " finger=" + fingerIndex);
    }
}
