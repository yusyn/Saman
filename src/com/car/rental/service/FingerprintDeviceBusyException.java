package com.car.rental.service;

/**
 * Another fingerprint operation (verify / enroll / delete) currently holds the device gate.
 */
public class FingerprintDeviceBusyException extends FingerprintException {

    public static final String CLIENT_MESSAGE = "دستگاه مشغول است";

    public FingerprintDeviceBusyException() {
        super(CLIENT_MESSAGE);
    }

    public FingerprintDeviceBusyException(String message) {
        super(message != null && !message.isBlank() ? message : CLIENT_MESSAGE);
    }

    public FingerprintDeviceBusyException(Throwable cause) {
        super(CLIENT_MESSAGE, cause);
    }
}
