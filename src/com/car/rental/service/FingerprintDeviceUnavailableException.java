package com.car.rental.service;

/**
 * Device is unreachable or did not complete ZK handshake within the configured timeout.
 * Distinct from enroll/verify operation timeouts and from "device busy".
 */
public class FingerprintDeviceUnavailableException extends FingerprintException {

    public static final String CLIENT_MESSAGE = "دستگاه اثر انگشت متصل نیست یا پاسخ نمی‌دهد";

    public FingerprintDeviceUnavailableException() {
        super(CLIENT_MESSAGE);
    }

    public FingerprintDeviceUnavailableException(String message) {
        super(message != null && !message.isBlank() ? message : CLIENT_MESSAGE);
    }

    public FingerprintDeviceUnavailableException(String message, Throwable cause) {
        super(message != null && !message.isBlank() ? message : CLIENT_MESSAGE, cause);
    }

    public FingerprintDeviceUnavailableException(Throwable cause) {
        super(CLIENT_MESSAGE, cause);
    }
}
