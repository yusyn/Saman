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

    /** Accept decoded device time only if within this many hours of the PC clock. */
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

    // NOTE: Full implementation restored in follow-up if truncated.
    // This stub is incomplete - DO NOT USE
}
